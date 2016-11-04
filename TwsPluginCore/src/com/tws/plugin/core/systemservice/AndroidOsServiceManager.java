package com.tws.plugin.core.systemservice;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

import tws.component.log.TwsLog;
import android.os.IBinder;

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
		Object androidOsServiceManagerProxy = RefInvoker.invokeStaticMethod("android.os.ServiceManager",
				"getIServiceManager", (Class[]) null, (Object[]) null);
		Object androidOsServiceManagerProxyProxy = ProxyUtil.createProxy(androidOsServiceManagerProxy,
				new AndroidOsServiceManager());
		RefInvoker.setStaticObject("android.os.ServiceManager", "sServiceManager", androidOsServiceManagerProxyProxy);

		// 干掉缓存
		sCache = (HashMap<String, IBinder>) RefInvoker.getFieldObject(null, "android.os.ServiceManager", "sCache");
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
