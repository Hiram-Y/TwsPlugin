package com.tws.plugin.core.systemservice;

import java.io.File;
import java.lang.reflect.Method;

import tws.component.log.TwsLog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.widget.RemoteViews;

import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginIntentResolver;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.manager.PluginManagerHelper;
import com.tws.plugin.core.proxy.MethodDelegate;
import com.tws.plugin.core.proxy.MethodProxy;
import com.tws.plugin.core.proxy.ProxyUtil;
import com.tws.plugin.util.FileUtil;
import com.tws.plugin.util.RefInvoker;
import com.tws.plugin.util.ResourceUtil;

/**
 * @author yongchen
 */
public class AndroidAppINotificationManager extends MethodProxy {

	private static final String TAG = "rick_Print:AndroidAppINotificationManager";

	static {
		sMethods.put("enqueueNotification", new enqueueNotification());
		sMethods.put("enqueueNotificationWithTag", new enqueueNotificationWithTag());
		sMethods.put("enqueueNotificationWithTagPriority", new enqueueNotificationWithTagPriority());
	}

    public static void installProxy() {
		TwsLog.d(TAG, "安装NotificationManagerProxy");
        Object androidAppINotificationStubProxy = RefInvoker.invokeStaticMethod(NotificationManager.class.getName(), "getService", (Class[])null, (Object[])null);
        Object androidAppINotificationStubProxyProxy = ProxyUtil.createProxy(androidAppINotificationStubProxy, new AndroidAppINotificationManager());
        RefInvoker.setStaticOjbect(NotificationManager.class.getName(), "sService", androidAppINotificationStubProxyProxy);
		TwsLog.d(TAG, "安装完成");
    }

    public static class enqueueNotification extends MethodDelegate {
        @Override
        public Object beforeInvoke(Object target, Method method, Object[] args) {
			TwsLog.e(TAG, "enqueueNotification beforeInvoke method:" + method.getName());
            args[0] = PluginLoader.getApplication().getPackageName();
            for(Object obj: args) {
                if (obj instanceof Notification) {
                    resolveRemoteViews((Notification)obj);
                    break;
                }
            }
            return super.beforeInvoke(target, method, args);
        }
    }

    public static class enqueueNotificationWithTag extends MethodDelegate {
        @Override
        public Object beforeInvoke(Object target, Method method, Object[] args) {
			TwsLog.e(TAG, "enqueueNotificationWithTag beforeInvoke method:" + method.getName());
            args[0] = PluginLoader.getApplication().getPackageName();
            for(Object obj: args) {
                if (obj instanceof Notification) {
                    resolveRemoteViews((Notification)obj);
                    break;
                }
            }
            return super.beforeInvoke(target, method, args);
        }
    }

    public static class enqueueNotificationWithTagPriority extends MethodDelegate {
        @Override
        public Object beforeInvoke(Object target, Method method, Object[] args) {
			TwsLog.e(TAG, "enqueueNotificationWithTagPriority beforeInvoke method:" + method.getName());
            args[0] = PluginLoader.getApplication().getPackageName();
            for(Object obj: args) {
                if (obj instanceof Notification) {
                    resolveRemoteViews((Notification)obj);
                    break;
                }
            }
            return super.beforeInvoke(target, method, args);
        }
    }

    private static void resolveRemoteViews(Notification notification) {
        if (Build.VERSION.SDK_INT >= 21) {

            int layoutId = 0;
            if (notification.contentView != null) {
                layoutId = (Integer)RefInvoker.getFieldObject(notification.contentView, RemoteViews.class, "mLayoutId");
            }
            if (layoutId == 0) {
                if (notification.bigContentView != null) {
                    layoutId = (Integer)RefInvoker.getFieldObject(notification.bigContentView, RemoteViews.class, "mLayoutId");
                }
            }
            if (layoutId != 0) {
                //检查资源布局资源Id是否属于宿主
                if (!ResourceUtil.isMainResId(layoutId)) {
                    //资源是来自插件
                    if (notification.contentIntent != null) {
                        Intent intent = (Intent)RefInvoker.invokeMethod(notification.contentIntent, PendingIntent.class.getName(), "getIntent", (Class[]) null, (Object[]) null);
                        if (intent.getAction() != null && intent.getAction().contains(PluginIntentResolver.CLASS_SEPARATOR)) {
                            String className = intent.getAction().split(PluginIntentResolver.CLASS_SEPARATOR)[0];
                            //通过重新构造ApplicationInfo来附加插件资源
                            PluginDescriptor pd = PluginManagerHelper.getPluginDescriptorByClassName(className);
                            if (pd != null) {
                                ApplicationInfo applicationInfo = PluginLoader.getApplication().getApplicationInfo();
                                ApplicationInfo newInfo = new ApplicationInfo();//重新构造一个，而不是修改原本的
                                newInfo.packageName = applicationInfo.packageName;
                                newInfo.sourceDir = applicationInfo.sourceDir;
                                newInfo.dataDir = applicationInfo.dataDir;
                                //要确保publicSourceDir这个路径可以被SystemUI应用读取，
                                newInfo.publicSourceDir = getNotificationResourcePath(pd.getInstalledPath(), PluginLoader.getApplication().getExternalCacheDir().getAbsolutePath() + "/notification_res.apk");
                                if (notification.tickerView != null) {
                                    RefInvoker.setFieldObject(notification.tickerView, RemoteViews.class.getName(), "mApplication", newInfo);
                                }
                                if (notification.contentView != null) {
                                    RefInvoker.setFieldObject(notification.contentView, RemoteViews.class.getName(), "mApplication", newInfo);
                                }
                                if (notification.bigContentView != null) {
                                    RefInvoker.setFieldObject(notification.bigContentView, RemoteViews.class.getName(), "mApplication", newInfo);
                                }
                                if (notification.headsUpContentView != null) {
                                    RefInvoker.setFieldObject(notification.headsUpContentView, RemoteViews.class.getName(), "mApplication", newInfo);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static String getNotificationResourcePath(String pluginInstalledPath, String worldReadablePath) {
		TwsLog.d(TAG, "正在为通知栏准备插件资源。。。这里现在暂时是同步复制，注意大文件卡顿！！");
        File worldReadableFile = new File(worldReadablePath);

        if (FileUtil.copyFile(pluginInstalledPath, worldReadableFile.getAbsolutePath())) {
			TwsLog.d(TAG, "通知栏插件资源准备完成，请确保此路径SystemUi有读权限:" + worldReadableFile.getAbsolutePath());
            return worldReadableFile.getAbsolutePath();
        } else {
			TwsLog.d(TAG, "不应该到这里来，直接返回这个路径SystemUi没有权限读取");
            return pluginInstalledPath;
        }
    }

}
