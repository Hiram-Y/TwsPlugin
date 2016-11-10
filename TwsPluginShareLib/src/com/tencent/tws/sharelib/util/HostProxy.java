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

	public static void notification(String className, String notificationValue, CharSequence title, CharSequence text) {
		if (sApplication == null) {
			return;
		}

		NotificationManager notificationManager = (NotificationManager) sApplication
				.getSystemService(sApplication.NOTIFICATION_SERVICE);
		Notification.Builder builder = new Notification.Builder(sApplication);

		Intent intent = new Intent();
		// 唤起指定Activity，这个应该换成宿主的
		intent.setClassName(sApplication.getPackageName(), className);
		// 还可以支持唤起service、receiver等等。
		intent.putExtra("param1", notificationValue);
		PendingIntent contentIndent = PendingIntent.getActivity(sApplication, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		final int id = getShareDrawableId("ic_launcher");
		builder.setContentIntent(contentIndent).setSmallIcon(id)// 设置状态栏里面的图标（小图标）
				// .setLargeIcon(BitmapFactory.decodeResource(res,R.drawable.i5))//下拉下拉列表里面的图标（大图标）
				// .setTicker("this is bitch!")//设置状态栏的显示的信息
				.setWhen(System.currentTimeMillis())// 设置时间发生时间
				.setAutoCancel(true)// 设置可以清除
				.setContentTitle(title)// 设置下拉列表里的标题
				.setDefaults(Notification.DEFAULT_SOUND)// 设置为默认的声音
				.setContentText(text);// 设置上下文内容

		// if (Build.VERSION.SDK_INT >= 21) {
		// // api大于等于21时，测试通知栏携带插件布局资源文件
		// builder.setContent(new RemoteViews(sApplication.getPackageName(),
		// R.layout.plugin_notification));
		//
		// }

		Notification notification = builder.getNotification();
		notificationManager.notify(id, notification);
	}
}
