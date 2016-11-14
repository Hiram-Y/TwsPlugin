package com.tws.plugin.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import tws.component.log.TwsLog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.widget.Toast;

import com.tws.plugin.core.PluginLoader;

public class FileUtil {

	private static final boolean DEBUG = false;
	private static final String TAG = "rick_Print:FileUtil";

	public static boolean copyFile(String source, String dest) {
		try {
			return copyFile(new FileInputStream(new File(source)), dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean copyFile(final InputStream inputStream, String dest) {
		TwsLog.d(TAG, "copyFile to " + dest);

		if (Build.VERSION.SDK_INT >= 23) {// Build.VERSION_CODES.M)
			if (dest.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
				// rick_Note Write code here^
				int permissionState = PackageManager.PERMISSION_GRANTED;// PluginLoader.getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if (permissionState != PackageManager.PERMISSION_GRANTED) {
					// 6.0的系统即使申请了读写sdcard的权限,仍然可以在设置中关闭, 则需要requestPermissons
					TwsLog.e(TAG, "6.0以上的系统, targetSDK>=23时, sdcard读写默认为未授权,需requestPermissons或者在设置中开启:" + dest);
					return false;
				}
			}
		}
		FileOutputStream oputStream = null;
		try {
			File destFile = new File(dest);
			File parentDir = destFile.getParentFile();
			if (!parentDir.isDirectory() || !parentDir.exists()) {
				destFile.getParentFile().mkdirs();
			}
			oputStream = new FileOutputStream(destFile);
			byte[] bb = new byte[48 * 1024];
			int len = 0;
			while ((len = inputStream.read(bb)) != -1) {
				oputStream.write(bb, 0, len);
			}
			oputStream.flush();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (oputStream != null) {
				try {
					oputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	public static boolean copySo(File sourceDir, String so, String dest) {

		try {

			boolean isSuccess = false;

			if (Build.VERSION.SDK_INT >= 21) {
				String[] abis = Build.SUPPORTED_ABIS;
				if (abis != null) {
					for (String abi : abis) {
						TwsLog.d(TAG, "try supported abi:" + abi);
						String name = "lib" + File.separator + abi + File.separator + so;
						File sourceFile = new File(sourceDir, name);
						if (sourceFile.exists()) {
							isSuccess = copyFile(sourceFile.getAbsolutePath(), dest + File.separator +  "lib" + File.separator + so);
							//api21 64位系统的目录可能有些不同
							//copyFile(sourceFile.getAbsolutePath(), dest + File.separator +  name);
							break;
						}
					}
				}
			} else {
				TwsLog.d(TAG, "supported api:" + Build.CPU_ABI + " " + Build.CPU_ABI2);

				String name = "lib" + File.separator + Build.CPU_ABI + File.separator + so;
				File sourceFile = new File(sourceDir, name);

				if (!sourceFile.exists() && Build.CPU_ABI2 != null) {
					name = "lib" + File.separator + Build.CPU_ABI2 + File.separator + so;
					sourceFile = new File(sourceDir, name);

					if (!sourceFile.exists()) {
						name = "lib" + File.separator + "armeabi" + File.separator + so;
						sourceFile = new File(sourceDir, name);
					}
				}
				if (sourceFile.exists()) {
					isSuccess = copyFile(sourceFile.getAbsolutePath(), dest + File.separator + "lib" + File.separator + so);
				}
			}

			if (!isSuccess) {
				TwsLog.e(TAG, "安装 :" + so + " 失败: NO_MATCHING_ABIS");
				if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
					Toast.makeText(PluginLoader.getApplication(), "安装 " + so + " 失败: NO_MATCHING_ABIS",
							Toast.LENGTH_SHORT).show();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	public static Set<String> unZipSo(String apkFile, File tempDir) {

		HashSet<String> result = null;

		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}

		TwsLog.d(TAG, "开始so文件:" + tempDir.getAbsolutePath());

		ZipFile zfile = null;
		boolean isSuccess = false;
		BufferedOutputStream fos = null;
		BufferedInputStream bis = null;
		try {
			zfile = new ZipFile(apkFile);
			ZipEntry ze = null;
			Enumeration zList = zfile.entries();
			while (zList.hasMoreElements()) {
				ze = (ZipEntry) zList.nextElement();
				String relativePath = ze.getName();

				if (!relativePath.startsWith("lib" + File.separator)) {
					if (DEBUG) {
						TwsLog.d(TAG, "不是lib目录，跳过:" + relativePath);
					}
					continue;
				}

				if (ze.isDirectory()) {
					File folder = new File(tempDir, relativePath);
					if (DEBUG) {
						TwsLog.d(TAG, "正在创建目录:" + folder.getAbsolutePath());
					}
					if (!folder.exists()) {
						folder.mkdirs();
					}

				} else {

					if (result == null) {
						result = new HashSet<String>(4);
					}

					File targetFile = new File(tempDir, relativePath);
					TwsLog.d(TAG, "正在解压so文件:" + targetFile.getAbsolutePath());
					if (!targetFile.getParentFile().exists()) {
						targetFile.getParentFile().mkdirs();
					}
					targetFile.createNewFile();

					fos = new BufferedOutputStream(new FileOutputStream(targetFile));
					bis = new BufferedInputStream(zfile.getInputStream(ze));
					byte[] buffer = new byte[2048];
					int count = -1;
					while ((count = bis.read(buffer)) != -1) {
						fos.write(buffer, 0, count);
						fos.flush();
					}
					fos.close();
					fos = null;
					bis.close();
					bis = null;

					result.add(relativePath.substring(relativePath.lastIndexOf(File.separator) + 1));
				}
			}
			isSuccess = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (zfile != null) {
				try {
					zfile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		TwsLog.d(TAG, "解压so文件结束 " + isSuccess);
		return result;
	}

	public static void readFileFromJar(String jarFilePath, String metaInfo) {
		TwsLog.d(TAG, "call readFileFromJar(" + jarFilePath + ", " + metaInfo + ")");
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jarFilePath);
			JarEntry entry = jarFile.getJarEntry(metaInfo);
			if (entry != null) {
				InputStream input = jarFile.getInputStream(entry);

				return;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return;

	}

	/**
	 * 递归删除文件及文件夹
	 * 
	 * @param file
	 */
	public static boolean deleteAll(File file) {
		if (file.isDirectory()) {
			File[] childFiles = file.listFiles();
			if (childFiles != null && childFiles.length > 0) {
				for (int i = 0; i < childFiles.length; i++) {
					deleteAll(childFiles[i]);
				}
			}
		}
		TwsLog.d(TAG, "delete:" + file.getAbsolutePath());
		return file.delete();
	}

	public static void printAll(File file) {
		TwsLog.d(TAG, "printAll:" + file.getAbsolutePath());
		if (file.isDirectory()) {
			File[] childFiles = file.listFiles();
			if (childFiles != null && childFiles.length > 0) {
				for (int i = 0; i < childFiles.length; i++) {
					printAll(childFiles[i]);
				}
			}
		}
	}

	public static String streamToString(InputStream input) throws IOException {

		InputStreamReader isr = new InputStreamReader(input);
		BufferedReader reader = new BufferedReader(isr);

		String line;
		StringBuffer sb = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		isr.close();
		return sb.toString();
	}

}
