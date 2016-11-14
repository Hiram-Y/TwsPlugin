package com.tws.plugin.core;

import tws.component.log.TwsLog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import dalvik.system.DexClassLoader;

/**
 * 为了支持Receiver和Service，增加此类。
 * 
 * @author yongchen
 * 
 */
public class HostClassLoader extends DexClassLoader {

	private static final String TAG = "rick_Print:HostClassLoader";

	public HostClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
		super(dexPath, optimizedDirectory, libraryPath, parent);
	}

	@Override
	public String findLibrary(String name) {
		TwsLog.d(TAG, "findLibrary:" + name);
		return super.findLibrary(name);
	}

	@Override
	protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {

		// Just for Receiver and Service
		TwsLog.d(TAG, "loadClass className:" + className + " resolve=" + resolve);

		if (className.startsWith(PluginIntentResolver.CLASS_PREFIX_SERVICE)) {

			TwsLog.d(TAG,
					"className is " + className + " PluginShadowService is " + PluginShadowService.class.getName());

			// 这里返回PluginShadowService是因为service的构造函数以及onCreate函数
			// 2个函数在ActivityThread的同一个函数中被调用,框架没机会在构造器执行之后,oncreate执行之前,
			// 插入一段代码, 注入context.
			// 因此这里返回一个fake的service, 在fake service的oncreate方法里面手动调用构造器和oncreate
			// 这里返回了这个Service以后,
			// 由于在框架中hook了ActivityManager的serviceDoneExecuting方法,
			// 在serviceDoneExecuting这个方法里面, 会将这个service再还原成插件的servcie对象
			if (className.equals(PluginIntentResolver.CLASS_PREFIX_SERVICE + "null")) {
				TwsLog.e(TAG, "到了这里说明出bug了,这里做个容错处理, 避免出现classnotfound");
				return TolerantService.class;
			} else {
				return PluginShadowService.class;
			}

		} else if (className.startsWith(PluginIntentResolver.CLASS_PREFIX_RECEIVER)) {

			String realName = className.replace(PluginIntentResolver.CLASS_PREFIX_RECEIVER, "");

			Class clazz = PluginLoader.loadPluginClassByName(realName);
			if (clazz != null) {
				TwsLog.d(TAG, "className is " + className + " target is " + realName
						+ (clazz == null ? " null" : " found"));
				return clazz;
			} else {
				TwsLog.e(TAG, "到了这里说明出bug了,这里做个容错处理, 避免出现classnotfound");
				return TolerantBroadcastReceiver.class;
			}
		}

		return super.loadClass(className, resolve);
	}

	public static class TolerantBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
		}
	}

	public static class TolerantService extends Service {
		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}
	}

}
