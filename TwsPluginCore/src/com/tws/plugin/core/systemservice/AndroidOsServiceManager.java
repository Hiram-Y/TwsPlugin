package com.tws.plugin.core.systemservice;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

import tws.component.log.TwsLog;
import android.os.Build;
import android.os.IBinder;
import android.view.ViewConfiguration;

import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.proxy.MethodDelegate;
import com.tws.plugin.core.proxy.MethodProxy;
import com.tws.plugin.core.proxy.ProxyUtil;
import com.tws.plugin.util.ProcessUtil;
import com.tws.plugin.util.RefInvoker;

public class AndroidOsServiceManager extends MethodProxy {

	public static final String TAG = "AndroidOsServiceManager";
	private static HashSet<String> sCacheKeySet;
	private static HashMap<String, IBinder> sCache;

	static {
		sMethods.put("getService", new getService());
	}

	public static void installProxy() {
		TwsLog.d(TAG, "安装IServiceManagerProxy");
		// for android 7.0 +
		if (Build.VERSION.SDK_INT > 23) {
			// 触发初始化WindowGlobal中的静态成员变量，
			// 避免7.＋的系统中对window服务代理，
			// 7.+的系统代理window服务会被SELinux拒绝导致陷入死循环
			ViewConfiguration.get(PluginLoader.getApplication());
		}
        
		Object androidOsServiceManagerProxy = RefInvoker.invokeMethod("android.os.ServiceManager",
				"getIServiceManager", (Class[]) null, (Object[]) null);
		Object androidOsServiceManagerProxyProxy = ProxyUtil.createProxy(androidOsServiceManagerProxy,
				new AndroidOsServiceManager());
		RefInvoker.setField("android.os.ServiceManager", "sServiceManager", androidOsServiceManagerProxyProxy);

		// 干掉缓存
		sCache = (HashMap<String, IBinder>) RefInvoker.getField(null, "android.os.ServiceManager", "sCache");
		sCacheKeySet = new HashSet<String>();
		sCacheKeySet.addAll(sCache.keySet());
		sCache.clear();

		TwsLog.d(TAG, "安装完成");
	}

	public static class getService extends MethodDelegate {

		@Override
		public Object afterInvoke(Object target, Method method, Object[] args, Object beforeInvoke, Object invokeResult) {
			if (invokeResult == null) {
				return super.afterInvoke(target, method, args, beforeInvoke, invokeResult);
			}

			TwsLog.d(TAG, "afterInvoke:" + method.getName() + " args is " + args[0]);
			if (ProcessUtil.isPluginProcess()) {
				IBinder binder = AndroidOsIBinder.installProxy((IBinder) invokeResult);
				// 补回安装时干掉的缓存
				if (sCacheKeySet.contains(args[0])) {
					sCache.put((String) args[0], binder);
				}
				return binder;
			}

			return super.afterInvoke(target, method, args, beforeInvoke, invokeResult);
		}
	}

}
