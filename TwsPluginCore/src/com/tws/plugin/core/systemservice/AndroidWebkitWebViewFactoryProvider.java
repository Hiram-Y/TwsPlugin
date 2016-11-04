package com.tws.plugin.core.systemservice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tws.component.log.TwsLog;
import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

import com.tws.plugin.core.proxy.MethodDelegate;
import com.tws.plugin.core.proxy.MethodProxy;
import com.tws.plugin.core.proxy.ProxyUtil;
import com.tws.plugin.util.RefInvoker;

/**
 * @author yongchen
 */
public class AndroidWebkitWebViewFactoryProvider extends MethodProxy {

	private static final String TAG = "rick_Print:AndroidWebkitWebViewFactoryProvider";
	static {
		sMethods.put("createWebView", new createWebView());
	}

	public static void installProxy() {
		if (Build.VERSION.SDK_INT >= 19) {
			TwsLog.d(TAG, "安装WebViewFactoryProviderProxy");
			// 在4。4及以上，这里的WebViewFactoryProvider的实际类型是
			// com.android.webview.chromium.WebViewChromiumFactoryProvider
			// implements WebViewFactoryProvider
			Object webViewFactoryProvider = RefInvoker.invokeMethod(null, "android.webkit.WebViewFactory",
					"getProvider", (Class[]) null, (Object[]) null);
			if (webViewFactoryProvider != null) {
				Object webViewFactoryProviderProxy = ProxyUtil.createProxy(webViewFactoryProvider,
						new AndroidWebkitWebViewFactoryProvider());
				RefInvoker.setStaticObject("android.webkit.WebViewFactory", "sProviderInstance",
						webViewFactoryProviderProxy);
			} else {
				// 如果取不到值，原因可能是不同版本差异
			}
			TwsLog.d(TAG, "安装完成");
		}
	}

	public static class createWebView extends MethodDelegate {

		@Override
		public Object afterInvoke(Object target, Method method, Object[] args, Object beforeInvoke,
				final Object invokeResult) {
			// 这里invokeResult的实际类型是com.android.webview.chromium.WebViewChromium
			// implements WebViewProvider
			// 所以这里可以再次进行Proxy
			final WebView webView = (WebView) args[0];
			fixWebViewAsset(webView.getContext());
			return super.afterInvoke(target, method, args, beforeInvoke, invokeResult);
		}
	}

	private static void fixWebViewAsset(Context context) {
		try {
			ClassLoader cl = null;
			if (sContextUtils == null) {
				if (sContentMain == null) {
					Object provider = RefInvoker.invokeMethod(null, "android.webkit.WebViewFactory", "getProvider",
							(Class[]) null, (Object[]) null);
					if (provider != null) {
						cl = provider.getClass().getClassLoader();

						try {
							sContentMain = Class.forName("org.chromium.content.app.ContentMain", true, cl);
						} catch (ClassNotFoundException e) {
						}

						if (sContentMain == null) {
							try {
								sContentMain = Class.forName("com.android.org.chromium.content.app.ContentMain", true,
										cl);
							} catch (ClassNotFoundException e) {
							}
						}

						if (sContentMain == null) {
							throw new ClassNotFoundException(String.format(
									"Can not found class %s or %s in classloader %s",
									"org.chromium.content.app.ContentMain",
									"com.android.org.chromium.content.app.ContentMain", cl));
						}
					}
				}
				if (sContentMain != null) {
					Class[] paramTypes = new Class[] { Context.class };
					try {
						Method method = sContentMain.getDeclaredMethod("initApplicationContext", paramTypes);
						if (method != null) {
							if (!method.isAccessible()) {
								method.setAccessible(true);
							}
							try {
								method.invoke(null, new Object[] { context.getApplicationContext() });
							} catch (IllegalAccessException e) {
							} catch (IllegalArgumentException e) {
							} catch (InvocationTargetException e) {
							}
						}
					} catch (NoSuchMethodException ex) {
						try {
							// compat for Chrome version 52
							sContextUtils = Class.forName("org.chromium.base.ContextUtils", true, cl);
						} catch (ClassNotFoundException e) {
						}
						if (sContextUtils != null) {
							RefInvoker.setFieldObject(null, sContextUtils, "sApplicationContext", null);
							RefInvoker.invokeMethod(null, sContextUtils, "initApplicationContext",
									new Class[] { Context.class }, new Object[] { context.getApplicationContext() });
						}
					}
				}
			} else {
				RefInvoker.setFieldObject(null, sContextUtils, "sApplicationContext", null);
				RefInvoker.invokeMethod(null, sContextUtils, "initApplicationContext", new Class[] { Context.class },
						new Object[] { context.getApplicationContext() });
				// 52.0.2743.98
				RefInvoker.invokeMethod(null, sContextUtils, "initApplicationContextForNative", (Class[]) null,
						(Object[]) null);
			}

		} catch (Exception e) {
			TwsLog.e(TAG, "createWebview", e);
		}
	}

	private static Class sContentMain;
	private static Class sContextUtils;

}