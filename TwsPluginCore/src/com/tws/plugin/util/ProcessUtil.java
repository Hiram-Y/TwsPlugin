package com.tws.plugin.util;

import tws.component.log.TwsLog;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;

import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.manager.PluginManagerProvider;

public class ProcessUtil {

	private static final String TAG = "ProcessUtil";
	private static Boolean isPluginProcess;

	public static boolean isPluginProcess(Context context) {

		if (isPluginProcess == null) {
			String processName = getCurProcessName(context);
			String pluginProcessName = getPluginProcessName(context);

			isPluginProcess = processName.equals(pluginProcessName);
		}

		return isPluginProcess;
	}

	public static boolean isPluginProcess() {
		return isPluginProcess(PluginLoader.getApplication());
	}

	public static boolean isHostProcess(Context context) {
		// 当前宿主和插件一个进程
		return isPluginProcess(context);
	}

	public static boolean isHostProcess() {
		return isHostProcess(PluginLoader.getApplication());
	}

	private static String getCurProcessName(Context context) {
		final int pid = android.os.Process.myPid();
		TwsLog.d(TAG, "getCurProcessName pid=" + pid);
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
			if (appProcess.pid == pid) {
				return appProcess.processName;
			}
		}
		return "";
	}

	private static String getPluginProcessName(Context context) {
		try {
			// 这里取个巧,直接查询ContentProvider的信息中包含的processName,因为Contentprovider是被配置在插件进程的
			ProviderInfo pinfo = context.getPackageManager().getProviderInfo(
					new ComponentName(context, PluginManagerProvider.class), 0);
			return pinfo.processName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return "";
	}
}
