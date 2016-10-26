package com.tws.plugin.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tws.component.log.TwsLog;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Window;

import com.tencent.tws.sharelib.annotation.AnnotationProcessor;
import com.tencent.tws.sharelib.annotation.PluginContainer;
import com.tencent.tws.sharelib.util.HostProxy;
import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginActivityInfo;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.content.PluginProviderInfo;
import com.tws.plugin.core.app.ActivityThread;
import com.tws.plugin.core.compat.CompatForSupportv7_23_2;
import com.tws.plugin.core.manager.PluginManagerHelper;
import com.tws.plugin.util.ProcessUtil;
import com.tws.plugin.util.RefInvoker;
import com.tws.plugin.util.ResourceUtil;

public class PluginInjector {

	private static final String TAG = "rick_Print:PluginInjector";

	private static final String android_content_ContextWrapper_mBase = "mBase";

	private static final String android_content_ContextThemeWrapper_attachBaseContext = "attachBaseContext";
	private static final String android_content_ContextThemeWrapper_mResources = "mResources";
	private static final String android_content_ContextThemeWrapper_mTheme = "mTheme";

	private static final String android_app_Activity_mInstrumentation = "mInstrumentation";
	private static final String android_app_Activity_mActivityInfo = "mActivityInfo";

	/**
	 * 替换宿主程序Application对象的mBase是为了修改它的几个StartActivity、
	 * StartService和SendBroadcast方法
	 */
	static void injectBaseContext(Context context) {
		TwsLog.d(TAG, "替换宿主程序Application对象的mBase");
		Context base = (Context) RefInvoker.getFieldObject(context, ContextWrapper.class.getName(),
				android_content_ContextWrapper_mBase);
		Context newBase = new PluginBaseContextWrapper(base);
		RefInvoker.setFieldObject(context, ContextWrapper.class.getName(), android_content_ContextWrapper_mBase,
				newBase);
	}

	/**
	 * 注入Instrumentation主要是为了支持Activity
	 */
	static void injectInstrumentation() {
		// 给Instrumentation添加一层代理，用来实现隐藏api的调用
		TwsLog.d(TAG, "替换宿主程序Intstrumentation");
		ActivityThread.wrapInstrumentation();
	}

	static void injectHandlerCallback() {
		TwsLog.d(TAG, "向宿主程序消息循环插入回调器");
		ActivityThread.wrapHandler();
	}

	public static void installContentProviders(Context context, Context pluginContext,
			Collection<PluginProviderInfo> pluginProviderInfos) {
		TwsLog.d(TAG, "安装插件ContentProvider:" + pluginContext.getPackageName() + " pluginProviderInfos.siz is "
				+ pluginProviderInfos.size());
		List<ProviderInfo> providers = new ArrayList<ProviderInfo>();
		for (PluginProviderInfo pluginProviderInfo : pluginProviderInfos) {
			ProviderInfo p = new ProviderInfo();
			// name做上标记，表示是来自插件，方便classloader进行判断
			p.name = pluginProviderInfo.getName();
			p.authority = pluginProviderInfo.getAuthority();
			p.applicationInfo = new ApplicationInfo(context.getApplicationInfo());
			p.applicationInfo.packageName = pluginContext.getPackageName();
			p.exported = pluginProviderInfo.isExported();
			p.packageName = context.getApplicationInfo().packageName;
			providers.add(p);
		}

		// pluginContext.getPackageName().equals(applicationInfo.packageName) ==
		// true
		ActivityThread.installContentProviders(pluginContext, providers);
	}

	static void injectInstrumetionFor360Safe(Activity activity, Instrumentation pluginInstrumentation) {
		// 检查mInstrumention是否已经替换成功。
		// 之所以要检查，是因为如果手机上安装了360手机卫士等app，它们可能会劫持用户app的ActivityThread对象，
		// 导致在PluginApplication的onCreate方法里面替换mInstrumention可能会失败
		// 所以这里再做一次检查
		Instrumentation instrumention = (Instrumentation) RefInvoker.getFieldObject(activity, Activity.class.getName(),
				android_app_Activity_mInstrumentation);
		if (!(instrumention instanceof PluginInstrumentionWrapper)) {
			// 说明被360还原了，这里再次尝试替换
			RefInvoker.setFieldObject(activity, Activity.class.getName(), android_app_Activity_mInstrumentation,
					pluginInstrumentation);
		}
	}

	static void injectActivityContext(Activity activity) {
		Intent intent = activity.getIntent();
		PluginContainer container = AnnotationProcessor.getPluginContainer(activity.getClass());
		// 如果是打开插件中的activity,
		// 或者是打开的用来显示插件组件的宿主activity
		boolean isStubActivity = PluginManagerHelper.isStub(intent.getComponent().getClassName());
		if (ProcessUtil.isPluginProcess() && (isStubActivity || container != null)) {

			// 在activityoncreate之前去完成attachBaseContext的事情

			Context pluginContext = null;
			PluginDescriptor pd = null;

			if (isStubActivity) {
				// 是打开插件中的activity

				pd = PluginManagerHelper.getPluginDescriptorByClassName(activity.getClass().getName());

				LoadedPlugin plugin = PluginLauncher.instance().getRunningPlugin(pd.getPackageName());

				pluginContext = PluginLoader.getNewPluginComponentContext(plugin.pluginContext,
						activity.getBaseContext(), 0);

				// 获取插件Application对象
				Application pluginApp = plugin.pluginApplication;

				// 重设mApplication
				RefInvoker.setFieldObject(activity, Activity.class.getName(), "mApplication", pluginApp);
			} else {
				// 是打开的用来显示插件组件的宿主activity

				if (!TextUtils.isEmpty(container.pluginId())) {
					// 进入这里表示指定了这个宿主Activity "只显示" 某个插件的组件
					// 因此直接将这个Activity的Context也替换成插件的Context
					pd = PluginManagerHelper.getPluginDescriptorByPluginId(container.pluginId());
					LoadedPlugin plugin = PluginLauncher.instance().getRunningPlugin(container.pluginId());
					pluginContext = PluginLoader.getNewPluginComponentContext(plugin.pluginContext,
							activity.getBaseContext(), 0);

				} else {
					// do nothing
					// 进入这里表示这个宿主可能要同时显示来自多个不同插件的组件,
					// 也就没办法将Context替换成之中某一个插件的context,
					// 剩下的交给PluginViewFactory去处理
					return;
				}

			}

			PluginActivityInfo pluginActivityInfo = pd.getActivityInfos().get(activity.getClass().getName());
			ActivityInfo activityInfo = (ActivityInfo) RefInvoker.getFieldObject(activity, Activity.class.getName(),
					android_app_Activity_mActivityInfo);
			int pluginAppTheme = getPluginTheme(activityInfo, pluginActivityInfo, pd);

			TwsLog.d(TAG, "Theme 0x" + Integer.toHexString(pluginAppTheme) + " activity:"
					+ activity.getClass().getName());

			resetActivityContext(pluginContext, activity, pluginAppTheme);

			resetWindowConfig(pluginContext, pd, activity, activityInfo, pluginActivityInfo);

			activity.setTitle(activity.getClass().getName());

		} else {
			// 如果是打开宿主程序的activity，注入一个无害的Context，用来在宿主程序中startService和sendBroadcast时检查打开的对象是否是插件中的对象
			// 插入Context
			Context mainContext = new PluginBaseContextWrapper(activity.getBaseContext());
			RefInvoker.setFieldObject(activity, ContextWrapper.class.getName(), android_content_ContextWrapper_mBase,
					null);
			RefInvoker.invokeMethod(activity, ContextThemeWrapper.class.getName(),
					android_content_ContextThemeWrapper_attachBaseContext, new Class[] { Context.class },
					new Object[] { mainContext });
		}
	}

	static void resetActivityContext(final Context pluginContext, final Activity activity, final int pluginAppTheme) {
		if (pluginContext == null) {
			return;
		}

		// 重设BaseContext
		RefInvoker.setFieldObject(activity, ContextWrapper.class.getName(), android_content_ContextWrapper_mBase, null);
		RefInvoker.invokeMethod(activity, ContextThemeWrapper.class.getName(),
				android_content_ContextThemeWrapper_attachBaseContext, new Class[] { Context.class },
				new Object[] { pluginContext });

		// 由于在attach的时候Resource已经被初始化了，所以需要重置Resource
		RefInvoker.setFieldObject(activity, ContextThemeWrapper.class.getName(),
				android_content_ContextThemeWrapper_mResources, null);

		CompatForSupportv7_23_2.fixResource(pluginContext, activity);

		// 重设theme
		if (pluginAppTheme != 0) {
			RefInvoker.setFieldObject(activity, ContextThemeWrapper.class.getName(),
					android_content_ContextThemeWrapper_mTheme, null);
			activity.setTheme(pluginAppTheme);
		}
		// 重设theme
		((PluginContextTheme) pluginContext).mTheme = null;
		pluginContext.setTheme(pluginAppTheme);

		// 重设mContext
		RefInvoker.setFieldObject(activity.getWindow(), Window.class.getName(), "mContext", pluginContext);

		// 重设mWindowStyle
		RefInvoker.setFieldObject(activity.getWindow(), Window.class.getName(), "mWindowStyle", null);

		// 重设LayoutInflater
		TwsLog.d(TAG, activity.getWindow().getClass().getName());
		RefInvoker.setFieldObject(activity.getWindow(), activity.getWindow().getClass().getName(), "mLayoutInflater",
				LayoutInflater.from(activity));

		// 如果api>=11,还要重设factory2
		if (Build.VERSION.SDK_INT >= 11) {
			RefInvoker.invokeMethod(activity.getWindow().getLayoutInflater(), LayoutInflater.class.getName(),
					"setPrivateFactory", new Class[] { LayoutInflater.Factory2.class }, new Object[] { activity });
		}
	}

	static void resetWindowConfig(final Context pluginContext, final PluginDescriptor pd, final Activity activity,
			final ActivityInfo activityInfo, final PluginActivityInfo pluginActivityInfo) {

		if (pluginActivityInfo != null) {

			// 如果PluginContextTheme的getPackageName返回了插件包名,需要在这里对attribute修正
			activity.getWindow().getAttributes().packageName = PluginLoader.getApplication().getPackageName();

			if (null != pluginActivityInfo.getWindowSoftInputMode()) {
				activity.getWindow().setSoftInputMode(
						Integer.parseInt(pluginActivityInfo.getWindowSoftInputMode().replace("0x", ""), 16));
			}
			if (Build.VERSION.SDK_INT >= 14) {
				if (null != pluginActivityInfo.getUiOptions()) {
					activity.getWindow().setUiOptions(
							Integer.parseInt(pluginActivityInfo.getUiOptions().replace("0x", ""), 16));
				}
			}
			if (null != pluginActivityInfo.getScreenOrientation()) {
				int orientation = Integer.parseInt(pluginActivityInfo.getScreenOrientation());
				// noinspection ResourceType
				if (orientation != activityInfo.screenOrientation && !activity.isChild()) {
					// noinspection ResourceType
					activity.setRequestedOrientation(orientation);
				}
			}
			if (Build.VERSION.SDK_INT >= 18 && !activity.isChild()) {
				Boolean isImmersive = ResourceUtil.getBoolean(pluginActivityInfo.getImmersive(), pluginContext);
				if (isImmersive != null) {
					activity.setImmersive(isImmersive);
				}
			}

			final String claName = activity.getClass().getName();
			TwsLog.d(TAG, claName + " immersive is " + pluginActivityInfo.getImmersive());
			TwsLog.d(TAG, claName + " screenOrientation is " + pluginActivityInfo.getScreenOrientation());
			TwsLog.d(TAG, claName + " launchMode is " + pluginActivityInfo.getLaunchMode());
			TwsLog.d(TAG, claName + " windowSoftInputMode is " + pluginActivityInfo.getWindowSoftInputMode());
			TwsLog.d(TAG, claName + " uiOptions is " + pluginActivityInfo.getUiOptions());
		}

		// 如果是独立插件，由于没有合并资源，这里还需要替换掉 mActivityInfo，
		// 避免activity试图通过ActivityInfo中的资源id来读取资源时失败
		activityInfo.icon = pd.getApplicationIcon();
		activityInfo.logo = pd.getApplicationLogo();
		if (Build.VERSION.SDK_INT >= 19) {
			activity.getWindow().setIcon(activityInfo.icon);
			activity.getWindow().setLogo(activityInfo.logo);
		}
	}

	/* package */static void replaceReceiverContext(Context baseContext, Context newBase) {

		if (baseContext.getClass().getName().equals("android.app.ContextImpl")) {
			ContextWrapper receiverRestrictedContext = (ContextWrapper) RefInvoker.invokeMethod(baseContext,
					"android.app.ContextImpl", "getReceiverRestrictedContext", (Class[]) null, (Object[]) null);
			RefInvoker.setFieldObject(receiverRestrictedContext, ContextWrapper.class.getName(), "mBase", newBase);
		}
	}

	// 这里是因为在多进程情况下，杀死插件进程，自动恢复service时有个bug导致一个service同时存在多个service实例
	// 这里做个遍历保护
	// break;
	/* package */static void replacePluginServiceContext(String serviceName) {
		Map<IBinder, Service> services = ActivityThread.getAllServices();
		if (services != null) {
			Iterator<Service> itr = services.values().iterator();
			while (itr.hasNext()) {
				Service service = itr.next();
				if (service != null && service.getClass().getName().equals(serviceName)) {

					replacePluginServiceContext(serviceName, service);
				}

			}
		}
	}

	/* package */static void replacePluginServiceContext(String servieName, Service service) {
		PluginDescriptor pd = PluginManagerHelper.getPluginDescriptorByClassName(servieName);

		LoadedPlugin plugin = PluginLauncher.instance().getRunningPlugin(pd.getPackageName());

		RefInvoker.setFieldObject(
				service,
				ContextWrapper.class.getName(),
				"mBase",
				PluginLoader.getNewPluginComponentContext(plugin.pluginContext, service.getBaseContext(),
						pd.getApplicationTheme()));

		RefInvoker.setFieldObject(service, Service.class.getName(), "mApplication", plugin.pluginApplication);

		RefInvoker.setFieldObject(service, Service.class, "mClassName",
				PluginManagerHelper.bindStubService(service.getClass().getName()));

	}

	/* package */static void replaceHostServiceContext(String serviceName) {
		Map<IBinder, Service> services = ActivityThread.getAllServices();
		if (services != null) {
			Iterator<Service> itr = services.values().iterator();
			while (itr.hasNext()) {
				Service service = itr.next();
				if (service != null && service.getClass().getName().equals(serviceName)) {
					PluginInjector.injectBaseContext(service);
					break;
				}

			}
		}
	}

	/**
	 * 主题的选择顺序为 先选择插件Activity配置的主题，再选择插件Application配置的主题，
	 * 如果是非独立插件，再选择宿主Activity主题 如果是独立插件，再选择系统默认主题
	 * 
	 * @param activityInfo
	 * @param pluginActivityInfo
	 * @param pd
	 * @return
	 */
	private static int getPluginTheme(ActivityInfo activityInfo, PluginActivityInfo pluginActivityInfo,
			PluginDescriptor pd) {
		int pluginAppTheme = 0;
		if ("samsung".equalsIgnoreCase(Build.BRAND)) {
			TwsLog.d(TAG, "getPluginTheme is samsung device！");
			pluginAppTheme = android.R.style.Theme_Holo_Light;
			// if (pluginActivityInfo != null) {
			// pluginAppTheme =
			// ResourceUtil.getResourceId(pluginActivityInfo.getTheme());
			// }
			// if (pluginAppTheme == 0) {
			// pluginAppTheme = pd.getApplicationTheme();
			// }
			//
			// if (pluginAppTheme == 0 && pd.isStandalone()) {
			// pluginAppTheme = android.R.style.Theme_Holo_Light;
			// }
			//
			// if (pluginAppTheme == 0) {
			// // If the activity defines a theme, that is used; else, the
			// // application theme is used.
			// pluginAppTheme = activityInfo.getThemeResource();
			// }
		} else {
			TwsLog.d(TAG, "getPluginTheme " + Build.BRAND + " use HostApplicationTheme!");
			pluginAppTheme = HostProxy.getHostApplicationThemeId();
			if (pluginAppTheme == 0 && pd.isStandalone()) {
				pluginAppTheme = android.R.style.Theme_Holo_Light;
			}
		}

		return pluginAppTheme;
	}

	/**
	 * 通常系统服务实例内部都有一个成员变量private final Context mContext;
	 * 
	 * 这个成员变量通常是一个ContextImpl实例。
	 * 
	 * @param manager
	 *            通过getSystemService获取的系统服务。例如 ActivityManager
	 * 
	 */
	static void replaceContext(Object manager, Context context) {
		Object original = RefInvoker.getFieldObject(manager, manager.getClass(), "mContext");
		if (original != null) {// 表示确实存在此成员变量对象，替换掉
			RefInvoker.setFieldObject(manager, manager.getClass().getName(), "mContext", context);
		}
	}

	/**
	 * 如果插件中不包含service、receiver，是不需要替换classloader的
	 */
	public static void hackHostClassLoaderIfNeeded() {
		Object mLoadedApk = RefInvoker.getFieldObject(PluginLoader.getApplication(), Application.class.getName(),
				"mLoadedApk");
		if (mLoadedApk == null) {
			// 重试一次
			mLoadedApk = RefInvoker.getFieldObject(PluginLoader.getApplication(), Application.class.getName(),
					"mLoadedApk");
		}
		if (mLoadedApk == null) {
			// 换个方式再试一次
			mLoadedApk = ActivityThread.getLoadedApk();
		}
		if (mLoadedApk != null) {
			ClassLoader originalLoader = (ClassLoader) RefInvoker.getFieldObject(mLoadedApk, "android.app.LoadedApk",
					"mClassLoader");
			if (!(originalLoader instanceof HostClassLoader)) {
				HostClassLoader newLoader = new HostClassLoader("", PluginLoader.getApplication().getCacheDir()
						.getAbsolutePath(), PluginLoader.getApplication().getCacheDir().getAbsolutePath(),
						originalLoader);
				RefInvoker.setFieldObject(mLoadedApk, "android.app.LoadedApk", "mClassLoader", newLoader);
			}
		} else {
			TwsLog.w(TAG, "What!!Why?");
		}
	}
}
