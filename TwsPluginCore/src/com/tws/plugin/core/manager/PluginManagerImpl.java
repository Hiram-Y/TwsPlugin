package com.tws.plugin.core.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tws.component.log.TwsLog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginCreator;
import com.tws.plugin.core.PluginLauncher;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.PluginManifestParser;
import com.tws.plugin.core.localservice.LocalServiceManager;
import com.tws.plugin.util.FileUtil;
import com.tws.plugin.util.PackageVerifyer;
import com.tws.plugin.util.ProcessUtil;

class PluginManagerImpl {

	private static final String TAG = "rick_Print:PluginManagerImpl";

	private static final boolean NEED_VERIFY_CERT = true;

	private static final String PLUGIN_SHAREED_PREFERENCE_NAME = "plugins.shared.preferences";

	private static final String INSTALLED_KEY = "plugins.list";
	private static final String PENDING_KEY = "plugins.pending";

	private static final String PLUGIN_DIR = "plugin_dir";
	private final Hashtable<String, PluginDescriptor> sInstalledPlugins = new Hashtable<String, PluginDescriptor>();
	private final Hashtable<String, PluginDescriptor> sPendingPlugins = new Hashtable<String, PluginDescriptor>();

	PluginManagerImpl() {
		if (!ProcessUtil.isPluginProcess()) {
			throw new IllegalAccessError("本类仅在插件进程使用");
		}
	}

	/**
	 * 插件的安装目录, 插件apk将来会被放在这个目录下面
	 */
	private String genInstallPath(String pluginId, String pluginVersoin) {
		return getPluginRootDir() + "/" + pluginId + "/" + pluginVersoin + "/base-1.apk";
	}

	private String getPluginRootDir() {
		return PluginLoader.getApplication().getDir(PLUGIN_DIR, Context.MODE_PRIVATE).getAbsolutePath();
	}

	@SuppressWarnings("unchecked")
	synchronized void loadInstalledPlugins() {
		if (sInstalledPlugins.size() == 0) {
			Hashtable<String, PluginDescriptor> installedPlugin = readPlugins(INSTALLED_KEY);
			if (installedPlugin != null) {
				sInstalledPlugins.putAll(installedPlugin);
			}

			// 把pending合并到install
			Hashtable<String, PluginDescriptor> pendingPlugin = readPlugins(PENDING_KEY);
			if (pendingPlugin != null) {
				Iterator<Map.Entry<String, PluginDescriptor>> itr = pendingPlugin.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, PluginDescriptor> entry = itr.next();
					// 删除旧版
					remove(entry.getKey());
				}

				// 保存新版
				sInstalledPlugins.putAll(pendingPlugin);
				savePlugins(INSTALLED_KEY, sInstalledPlugins);

				// 清除pending
				getSharedPreference().edit().remove(PENDING_KEY).commit();
			}

		}
	}

	private boolean addOrReplace(PluginDescriptor pluginDescriptor) {
		sInstalledPlugins.put(pluginDescriptor.getPackageName(), pluginDescriptor);
		return savePlugins(INSTALLED_KEY, sInstalledPlugins);
	}

	private boolean pending(PluginDescriptor pluginDescriptor) {
		sPendingPlugins.put(pluginDescriptor.getPackageName(), pluginDescriptor);
		return savePlugins(PENDING_KEY, sPendingPlugins);
	}

	synchronized boolean removeAll() {
		PluginManagerHelper.clearLocalCache();
		sInstalledPlugins.clear();
		boolean isSuccess = savePlugins(INSTALLED_KEY, sInstalledPlugins);

		FileUtil.deleteAll(new File(getPluginRootDir()));

		return isSuccess;
	}

	synchronized boolean remove(String pluginId) {
		PluginManagerHelper.clearLocalCache();

		PluginDescriptor old = sInstalledPlugins.remove(pluginId);

		boolean result = false;

		if (old != null) {
			PluginLauncher.instance().stopPlugin(pluginId, old);
			result = savePlugins(INSTALLED_KEY, sInstalledPlugins);
			boolean deleteSuccess = FileUtil.deleteAll(new File(old.getInstalledPath()).getParentFile());
			TwsLog.d(TAG, "delete old result:" + result + " deleteSuccess=" + deleteSuccess + " old.getInstalledPath="
					+ old.getInstalledPath() + " old.getPackageName=" + old.getPackageName());
		} else {
			TwsLog.d(TAG, "插件未安装：" + pluginId);
		}

		return result;
	}

	Collection<PluginDescriptor> getPlugins() {
		return sInstalledPlugins.values();
	}

	/**
	 * for Fragment
	 * 
	 * @param clazzId
	 * @return
	 */
	PluginDescriptor getPluginDescriptorByFragmenetId(String clazzId) {
		Iterator<PluginDescriptor> itr = sInstalledPlugins.values().iterator();
		while (itr.hasNext()) {
			PluginDescriptor descriptor = itr.next();
			if (descriptor.containsFragment(clazzId)) {
				return descriptor;
			}
		}
		return null;
	}

	PluginDescriptor getPluginDescriptorByPluginId(String pluginId) {
		PluginDescriptor pluginDescriptor = sInstalledPlugins.get(pluginId);
		if (pluginDescriptor != null && pluginDescriptor.isEnabled()) {
			return pluginDescriptor;
		}
		return null;
	}

	PluginDescriptor getPluginDescriptorByClassName(String clazzName) {
		Iterator<PluginDescriptor> itr = sInstalledPlugins.values().iterator();
		while (itr.hasNext()) {
			PluginDescriptor descriptor = itr.next();
			if (descriptor.containsName(clazzName)) {
				return descriptor;
			}
		}
		return null;
	}

	/**
	 * 安装一个插件
	 * 
	 * @param srcPluginFile
	 * @return
	 */
	synchronized InstallResult installPlugin(String srcPluginFile) {
		TwsLog.d(TAG, "开始安装插件:" + srcPluginFile);
		if (TextUtils.isEmpty(srcPluginFile) || !new File(srcPluginFile).exists()) {
			return new InstallResult(InstallResult.SRC_FILE_NOT_FOUND);
		}

		// 第0步，先将apk复制到宿主程序私有目录，防止在安装过程中文件被篡改
		if (!srcPluginFile.startsWith(PluginLoader.getApplication().getCacheDir().getAbsolutePath())) {
			String tempFilePath = PluginLoader.getApplication().getCacheDir().getAbsolutePath() + File.separator
					+ System.currentTimeMillis() + ".apk";
			if (FileUtil.copyFile(srcPluginFile, tempFilePath)) {
				srcPluginFile = tempFilePath;
			} else {
				TwsLog.e(TAG, "复制插件文件失败 srcPluginFile=" + srcPluginFile + " tempFilePath=" + tempFilePath);
				return new InstallResult(InstallResult.COPY_FILE_FAIL);
			}
		}

		// 第1步，验证插件APK签名，如果被篡改过，将获取不到证书
		// sApplication.getPackageManager().getPackageArchiveInfo(srcPluginFile,
		// PackageManager.GET_SIGNATURES);
		Signature[] pluginSignatures = PackageVerifyer.collectCertificates(srcPluginFile, false);
		boolean isDebugable = (0 != (PluginLoader.getApplication().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
		if (pluginSignatures == null) {
			TwsLog.e(TAG, "插件签名验证失败:" + srcPluginFile);
			new File(srcPluginFile).delete();
			return new InstallResult(InstallResult.SIGNATURES_INVALIDATE);
		} else if (NEED_VERIFY_CERT && !isDebugable) {
			// 可选步骤，验证插件APK证书是否和宿主程序证书相同。
			// 证书中存放的是公钥和算法信息，而公钥和私钥是1对1的
			// 公钥相同意味着是同一个作者发布的程序
			Signature[] mainSignatures = null;
			try {
				PackageInfo pkgInfo = PluginLoader.getApplication().getPackageManager()
						.getPackageInfo(PluginLoader.getApplication().getPackageName(), PackageManager.GET_SIGNATURES);
				mainSignatures = pkgInfo.signatures;
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
			if (!PackageVerifyer.isSignaturesSame(mainSignatures, pluginSignatures)) {
				TwsLog.e(TAG, "插件证书和宿主证书不一致:" + srcPluginFile);
				new File(srcPluginFile).delete();
				return new InstallResult(InstallResult.VERIFY_SIGNATURES_FAIL);
			}
		}

		// 第2步，解析Manifest，获得插件详情
		PluginDescriptor pluginDescriptor = PluginManifestParser.parseManifest(srcPluginFile);
		if (pluginDescriptor == null || TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
			TwsLog.e(TAG, "解析插件Manifest文件失败:" + srcPluginFile);
			new File(srcPluginFile).delete();
			return new InstallResult(InstallResult.PARSE_MANIFEST_FAIL);
		}

		// 判断插件适用系统版本
		if (pluginDescriptor.getMinSdkVersion() != null
				&& Build.VERSION.SDK_INT < Integer.valueOf(pluginDescriptor.getMinSdkVersion())) {
			TwsLog.e(TAG,
					"当前系统版本过低, 不支持此插件 系统v:" + Build.VERSION.SDK_INT + " 插件v:" + pluginDescriptor.getMinSdkVersion());
			new File(srcPluginFile).delete();
			return new InstallResult(InstallResult.MIN_API_NOT_SUPPORTED, pluginDescriptor.getPackageName(),
					pluginDescriptor.getVersion());
		}

		PackageInfo packageInfo = PluginLoader.getApplication().getPackageManager()
				.getPackageArchiveInfo(srcPluginFile, PackageManager.GET_GIDS);
		if (packageInfo != null) {
			pluginDescriptor.setApplicationTheme(packageInfo.applicationInfo.theme);
			pluginDescriptor.setApplicationIcon(packageInfo.applicationInfo.icon);
			pluginDescriptor.setApplicationLogo(packageInfo.applicationInfo.logo);
		}

		// 第3步，检查插件是否已经存在,若存在删除旧的
		PluginDescriptor oldPluginDescriptor = getPluginDescriptorByPluginId(pluginDescriptor.getPackageName());
		if (oldPluginDescriptor != null) {
			TwsLog.w(TAG, "已安装过，安装路径为:" + oldPluginDescriptor.getInstalledPath() + " oldPluginVer:"
					+ oldPluginDescriptor.getVersion() + " pluginVer:" + pluginDescriptor.getVersion());

			// 检查插件是否已经加载
			if (PluginLauncher.instance().isRunning(oldPluginDescriptor.getPackageName())) {
				if (!oldPluginDescriptor.getVersion().equals(pluginDescriptor.getVersion())) {
					TwsLog.w(TAG, "旧版插件已经加载， 且新版插件和旧版插件版本不同，直接删除旧版，进行热更新");
					remove(oldPluginDescriptor.getPackageName());
				} else {
					TwsLog.w(TAG, "旧版插件已经加载， 且新版插件和旧版插件版本相同，拒绝安装");
					new File(srcPluginFile).delete();
					return new InstallResult(InstallResult.FAIL_BECAUSE_HAS_LOADED, pluginDescriptor.getPackageName(),
							pluginDescriptor.getVersion());
				}
			} else {
				TwsLog.w(TAG, "旧版插件还未加载，忽略版本，直接删除旧版，尝试安装新版");
				remove(oldPluginDescriptor.getPackageName());
			}
		}

		// 第4步骤，复制插件到插件目录
		String destApkPath = genInstallPath(pluginDescriptor.getPackageName(), pluginDescriptor.getVersion());
		boolean isCopySuccess = FileUtil.copyFile(srcPluginFile, destApkPath);

		if (!isCopySuccess) {

			TwsLog.e(TAG, "复制插件到安装目录失败 srcPluginFile is " + srcPluginFile);
			// 删掉临时文件
			new File(srcPluginFile).delete();
			return new InstallResult(InstallResult.COPY_FILE_FAIL, pluginDescriptor.getPackageName(),
					pluginDescriptor.getVersion());
		} else {

			// 第5步，先解压so到临时目录，再从临时目录复制到插件so目录。
			// 在构造插件Dexclassloader的时候，会使用这个so目录作为参数
			File apkParent = new File(destApkPath).getParentFile();
			File tempSoDir = new File(apkParent, "temp");
			Set<String> soList = FileUtil.unZipSo(srcPluginFile, tempSoDir);
			if (soList != null) {
				for (String soName : soList) {
					FileUtil.copySo(tempSoDir, soName, apkParent.getAbsolutePath());
				}
				// 删掉临时文件
				FileUtil.deleteAll(tempSoDir);
			}

			// try {
			// ArrayList<String> multiDexFiles =
			// PluginMultiDexExtractor.performExtractions(new File(destApkPath),
			// new File(apkParent, "secondDexes"));
			// pluginDescriptor.setMuliDexList(multiDexFiles);
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			// 第6步 添加到已安装插件列表
			pluginDescriptor.setInstalledPath(destApkPath);
			boolean isInstallSuccess = false;

			isInstallSuccess = addOrReplace(pluginDescriptor);

			// 删掉临时文件
			new File(srcPluginFile).delete();

			if (!isInstallSuccess) {
				TwsLog.e(TAG, "安装插件失败:" + srcPluginFile);

				new File(destApkPath).delete();

				return new InstallResult(InstallResult.INSTALL_FAIL, pluginDescriptor.getPackageName(),
						pluginDescriptor.getVersion());
			} else {
				// 通过创建classloader来触发dexopt，但不加载
				TwsLog.d(TAG, "正在进行DEXOPT..." + pluginDescriptor.getInstalledPath());
				// ActivityThread.getPackageManager().performDexOptIfNeeded()
				FileUtil.deleteAll(new File(apkParent, "dalvik-cache"));
				ClassLoader cl = PluginCreator.createPluginClassLoader(pluginDescriptor.getInstalledPath(),
						pluginDescriptor.isStandalone(), null, null);
				try {
					cl.loadClass(Object.class.getName());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				TwsLog.d(TAG, "DEXOPT完毕");

				LocalServiceManager.registerService(pluginDescriptor);

				TwsLog.d(TAG, "安装插件成功:" + destApkPath);

				// 打印一下目录结构
				if (isDebugable) {
					FileUtil.printAll(new File(PluginLoader.getApplication().getApplicationInfo().dataDir));
				}

				return new InstallResult(InstallResult.SUCCESS, pluginDescriptor.getPackageName(),
						pluginDescriptor.getVersion());
			}
		}
	}

	private static SharedPreferences getSharedPreference() {
		SharedPreferences sp = PluginLoader.getApplication().getSharedPreferences(PLUGIN_SHAREED_PREFERENCE_NAME,
				Build.VERSION.SDK_INT < 11 ? Context.MODE_PRIVATE : Context.MODE_PRIVATE | 0x0004);
		return sp;
	}

	private synchronized boolean savePlugins(String key, Hashtable<String, PluginDescriptor> plugins) {

		ObjectOutputStream objectOutputStream = null;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(plugins);
			objectOutputStream.flush();

			byte[] data = byteArrayOutputStream.toByteArray();
			String list = Base64.encodeToString(data, Base64.DEFAULT);

			getSharedPreference().edit().putString(key, list).commit();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (objectOutputStream != null) {
				try {
					objectOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (byteArrayOutputStream != null) {
				try {
					byteArrayOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private synchronized Hashtable<String, PluginDescriptor> readPlugins(String key) {
		String list = getSharedPreference().getString(key, "");
		Serializable object = null;
		if (!TextUtils.isEmpty(list)) {
			ByteArrayInputStream byteArrayInputStream = null;
			ObjectInputStream objectInputStream = null;
			try {
				byteArrayInputStream = new ByteArrayInputStream(Base64.decode(list, Base64.DEFAULT));
				objectInputStream = new ObjectInputStream(byteArrayInputStream);
				object = (Serializable) objectInputStream.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (objectInputStream != null) {
					try {
						objectInputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (byteArrayInputStream != null) {
					try {
						byteArrayInputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return (Hashtable<String, PluginDescriptor>) object;
	}
}