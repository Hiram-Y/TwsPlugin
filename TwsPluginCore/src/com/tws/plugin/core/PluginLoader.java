package com.tws.plugin.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tws.component.log.TwsLog;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.tencent.tws.sharelib.util.HostProxy;
import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.app.AndroidViewLayoutInflater;
import com.tws.plugin.core.compat.CompatForSupportv7ViewInflater;
import com.tws.plugin.core.manager.PluginManagerHelper;
import com.tws.plugin.core.systemservice.AndroidAppIActivityManager;
import com.tws.plugin.core.systemservice.AndroidAppINotificationManager;
import com.tws.plugin.core.systemservice.AndroidAppIPackageManager;
import com.tws.plugin.core.systemservice.AndroidOsServiceManager;
import com.tws.plugin.core.systemservice.AndroidWebkitWebViewFactoryProvider;
import com.tws.plugin.util.ProcessUtil;

import dalvik.system.DexClassLoader;

public class PluginLoader {

	private static final String TAG = "rick_Print:PluginLoader";

	private static Application sApplication;
	private static boolean isLoaderInited = false;

	private PluginLoader() {
	}

	public static Application getApplication() {
		if (sApplication == null) {
			throw new IllegalStateException("框架尚未初始化，请确定在当前进程中，PluginLoader.initLoader方法已执行！");
		}
		return sApplication;
	}

	/**
	 * 初始化loader, 只可调用一次
	 * 
	 * @param app
	 */
	public static synchronized void initLoader(Application app) {

		if (!isLoaderInited) {
			TwsLog.d(TAG, "插件框架初始化中...");

			long t1 = System.currentTimeMillis();

			isLoaderInited = true;
			sApplication = app;
			HostProxy.setApplication(sApplication);

			// 这里的isPluginProcess方法需要在安装AndroidAppIActivityManager之前执行一次。
			// 原因见AndroidAppIActivityManager的getRunningAppProcesses()方法
			boolean isPluginProcess = ProcessUtil.isPluginProcess();
			if (isPluginProcess) {
				AndroidOsServiceManager.installProxy();
			}

			AndroidAppIActivityManager.installProxy();
			AndroidAppINotificationManager.installProxy();
			AndroidAppIPackageManager.installProxy(sApplication.getPackageManager());

			if (isPluginProcess) {
				AndroidViewLayoutInflater.installPluginCustomViewConstructorCache();
				CompatForSupportv7ViewInflater.installPluginCustomViewConstructorCache();
				// 不可在主进程中同步安装，因为此时ActivityThread还没有准备好, 会导致空指针。
				new Handler().post(new Runnable() {
					@Override
					public void run() {
						AndroidWebkitWebViewFactoryProvider.installProxy();
					}
				});
			}

			PluginInjector.injectHandlerCallback();// 本来宿主进程是不需要注入handlecallback的，这里加上是为了对抗360安全卫士等软件，提高Instrumentation的成功率
			PluginInjector.injectInstrumentation();
			PluginInjector.injectBaseContext(sApplication);

			if (isPluginProcess) {
				if (Build.VERSION.SDK_INT >= 14) {
					sApplication.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
						@Override
						public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
						}

						@Override
						public void onActivityStarted(Activity activity) {
						}

						@Override
						public void onActivityResumed(Activity activity) {
						}

						@Override
						public void onActivityPaused(Activity activity) {
						}

						@Override
						public void onActivityStopped(Activity activity) {
						}

						@Override
						public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
						}

						@Override
						public void onActivityDestroyed(Activity activity) {
							Intent intent = activity.getIntent();
							if (intent != null && intent.getComponent() != null) {
								PluginManagerHelper.unBindLaunchModeStubActivity(intent.getComponent().getClassName(),
										activity.getClass().getName());
							}
						}
					});
				}
			}
			long t2 = System.currentTimeMillis();
			TwsLog.d(TAG, "插件框架初始化完成  耗时：" + (t2 - t1));
		}
	}

	public static Context fixBaseContextForReceiver(Context superApplicationContext) {
		if (superApplicationContext instanceof ContextWrapper) {
			return ((ContextWrapper) superApplicationContext).getBaseContext();
		} else {
			return superApplicationContext;
		}
	}

	/**
	 * 根据插件中的classId加载一个插件中的class
	 * 
	 * @param clazzId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Class loadPluginFragmentClassById(String clazzId) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByFragmentId(clazzId);
		if (pluginDescriptor != null) {
			// 插件可能尚未初始化，确保使用前已经初始化
			LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

			DexClassLoader pluginClassLoader = plugin.pluginClassLoader;

			String clazzName = pluginDescriptor.getPluginClassNameById(clazzId);
			if (clazzName != null) {
				try {
					Class pluginClazz = ((ClassLoader) pluginClassLoader).loadClass(clazzName);
					TwsLog.d(TAG, "loadPluginClass for clazzId:" + clazzId + " clazzName=" + clazzName + " success");
					return pluginClazz;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		TwsLog.e(TAG, "loadPluginClass for clazzId:" + clazzId + " fail");

		return null;

	}

	@SuppressWarnings("rawtypes")
	public static Class loadPluginClassByName(String clazzName) {

		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(clazzName);

		if (pluginDescriptor != null) {
			// 插件可能尚未初始化，确保使用前已经初始化
			LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

			DexClassLoader pluginClassLoader = plugin.pluginClassLoader;

			try {
				Class pluginClazz = ((ClassLoader) pluginClassLoader).loadClass(clazzName);
				TwsLog.d(TAG, "loadPluginClass Success for clazzName is " + clazzName);
				return pluginClazz;
			} catch (ClassNotFoundException e) {
				TwsLog.e(TAG, "ClassNotFound " + clazzName, e);
			} catch (java.lang.IllegalAccessError illegalAccessError) {
				illegalAccessError.printStackTrace();
				throw new IllegalAccessError("出现这个异常最大的可能是插件dex和" + "宿主dex包含了相同的class导致冲突, "
						+ "请检查插件的编译脚本，确保排除了所有公共依赖库的jar");
			}

		}

		return null;

	}

	/**
	 * 获取当前class所在插件的Context 每个插件只有1个DefaultContext, 是当前插件中所有class公用的Context
	 * 
	 * @param clazz
	 * @return
	 */
	public static Context getDefaultPluginContext(@SuppressWarnings("rawtypes") Class clazz) {

		Context pluginContext = null;
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(clazz.getName());

		if (pluginDescriptor != null) {
			pluginContext = PluginLauncher.instance().getRunningPlugin(pluginDescriptor.getPackageName()).pluginContext;
			;
		} else {
			TwsLog.e(TAG, "PluginDescriptor Not Found for " + clazz.getName());
		}

		if (pluginContext == null) {
			TwsLog.e(TAG, "Context Not Found for " + clazz.getName());
		}

		return pluginContext;
	}

	/**
	 * 根据当前插件的默认Context, 为当前插件的组件创建一个单独的context
	 * 
	 * @param pluginContext
	 * @param base
	 *            由系统创建的Context。 其实际类型应该是ContextImpl
	 * @return
	 */
	/* package */static Context getNewPluginComponentContext(Context pluginContext, Context base, int theme) {
		PluginContextTheme newContext = null;
		if (pluginContext != null) {
			newContext = (PluginContextTheme) PluginCreator.createPluginContext(
					((PluginContextTheme) pluginContext).getPluginDescriptor(), base, pluginContext.getResources(),
					(DexClassLoader) pluginContext.getClassLoader());

			newContext.setPluginApplication((Application) ((PluginContextTheme) pluginContext).getApplicationContext());

			newContext.setTheme(sApplication.getApplicationContext().getApplicationInfo().theme);
		}
		return newContext;
	}

	public static Context getNewPluginApplicationContext(String pluginId) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(pluginId);

		// 插件可能尚未初始化，确保使用前已经初始化
		LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

		if (plugin != null) {
			PluginContextTheme newContext = (PluginContextTheme) PluginCreator.createPluginContext(
					((PluginContextTheme) plugin.pluginContext).getPluginDescriptor(), sApplication.getBaseContext(),
					plugin.pluginResource, plugin.pluginClassLoader);

			newContext.setPluginApplication(plugin.pluginApplication);

			newContext.setTheme(pluginDescriptor.getApplicationTheme());

			return newContext;
		}

		return null;
	}

	public static boolean isInstalled(String pluginId, String pluginVersion) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(pluginId);
		if (pluginDescriptor != null) {
			TwsLog.d(
					TAG,
					"call isInstalled pluginId=" + pluginId + " pluginDescriptor.getVersion="
							+ pluginDescriptor.getVersion() + " pluginVersion=" + pluginVersion);
			return pluginDescriptor.getVersion().equals(pluginVersion);
		}
		return false;
	}

	/**
	 */
	public static ArrayList<String> matchPlugin(Intent intent, int type) {
		ArrayList<String> result = null;

		String packageName = intent.getPackage();
		if (packageName == null && intent.getComponent() != null) {
			packageName = intent.getComponent().getPackageName();
		}
		if (packageName != null && !packageName.equals(PluginLoader.getApplication().getPackageName())) {
			PluginDescriptor dp = PluginManagerHelper.getPluginDescriptorByPluginId(packageName);
			if (dp != null) {
				List<String> list = dp.matchPlugin(intent, type);
				if (list != null && list.size() > 0) {
					if (result == null) {
						result = new ArrayList<String>();
					}
					result.addAll(list);
				}
			}
		} else {
			Iterator<PluginDescriptor> itr = PluginManagerHelper.getPlugins().iterator();
			while (itr.hasNext()) {
				List<String> list = itr.next().matchPlugin(intent, type);
				if (list != null && list.size() > 0) {
					if (result == null) {
						result = new ArrayList<String>();
					}
					result.addAll(list);
				}
				if (result != null && type != PluginDescriptor.BROADCAST) {
					break;
				}
			}

		}
		return result;
	}

}
