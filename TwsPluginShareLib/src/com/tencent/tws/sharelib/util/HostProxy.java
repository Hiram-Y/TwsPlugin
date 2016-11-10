package com.tencent.tws.sharelib.util;

import tws.component.log.TwsLog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class HostProxy {
	private static Application sApplication = null;
	private static String HOST_PACKAGE_NAME = "com.example.pluginhost";

	public static void setApplication(Application context) {
		sApplication = context;
	}

	public static Application getApplication() {
		if (sApplication == null) {
			throw new IllegalStateException("框架尚未初始化，请确定在当前进程中的PluginLoader.initLoader方法已执行！");
		}
		return sApplication;
	}

	public static int getHostApplicationThemeId() {
		return sApplication.getApplicationInfo().theme;
	}

	public static int getShareStyleId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "style", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareStyleId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareAttrId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "attr", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareAttrId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareDrawableId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "drawable", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareDrawableId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareLayoutId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "layout", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareLayoutId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareDimenId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "dimen", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareDimenId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareStringId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "string", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareStringId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareColorId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "color", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareColorId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareBoolId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "bool", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareBoolId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareIntegerId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "integer", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareIntegerId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}
}
