package com.tws.plugin.core;

import tws.component.log.TwsLog;
import android.app.Application;
import android.content.Context;

import com.tws.plugin.core.localservice.LocalServiceManager;
import com.tws.plugin.util.ProcessUtil;

public class PluginApplication extends Application {

	private static final String TAG = "rick_Print:PluginApplication";
	private static PluginApplication instance;

	public static PluginApplication getInstance() {
		return instance;
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		instance = this;
		// 这个地方之所以这样写，是因为如果是插件进程，initPluginFramework必须在applicaiotn启动时执行
		// 而如果是宿主进程，initPluginFramework可以在这里执行，也可以在需要时再在宿主的其他组件中执行，
		// 例如点击宿主的某个Activity中的button后再执行这个方法来启动插件框架。

		// 总体原则有3点：
		// 1、插件进程和宿主进程都必须有机会执行initPluginFramework
		// 2、在插件进程和宿主进程的initPluginFramework方法都执行完毕之前，不可和插件交互
		// 3、在插件进程和宿主进程的initPluginFramework方法都执行完毕之前启动的组件，即使在initPluginFramework都执行完毕之后，也不可和插件交互

		// 如果initPluginFramework都在进程启动时就执行，自然很轻松满足上述条件。
		if (ProcessUtil.isPluginProcess(this)) {
			TwsLog.d(TAG, "插件进行 PluginLoader.initPluginFramework");
			// 插件进程，必须在这里执行initPluginFramework
			PluginLoader.initPluginFramework(this);
		} else {
			// 宿主进程，可以在这里执行，也可以选择在宿主的其他地方在需要时再启动插件框架
			TwsLog.d(TAG, "宿主进行 PluginLoader.initPluginFramework");
			PluginLoader.initPluginFramework(this);
		}
		// init ServiceManager
		LocalServiceManager.init();
	}

	/**
	 * 重写这个方法是为了支持Receiver,否则会出现ClassCast错误
	 */
	@Override
	public Context getBaseContext() {
		return PluginLoader.fixBaseContextForReceiver(super.getBaseContext());
	}

	@Override
	public void onCreate() {
		super.onCreate();

		TwsLog.registerLogReceiver(this);
		if (ProcessUtil.isHostProcess(this)) {
			PluginLoader.loadPlugins(this);
		}
	}
}
