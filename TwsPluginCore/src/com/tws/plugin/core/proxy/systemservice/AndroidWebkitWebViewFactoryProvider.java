package com.tws.plugin.core.proxy.systemservice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tws.component.log.TwsLog;
import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

import com.tws.plugin.core.android.HackWebViewFactory;
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
			// 在4.4及以上，这里的WebViewFactoryProvider的实际类型是
			// com.android.webview.chromium.WebViewChromiumFactoryProvider
			// implements WebViewFactoryProvider
			Object webViewFactoryProvider = HackWebViewFactory.getProvider();
			if (webViewFactoryProvider != null) {
				Object webViewFactoryProviderProxy = ProxyUtil.createProxy(webViewFactoryProvider,
						new AndroidWebkitWebViewFactoryProvider());
				HackWebViewFactory.setProviderInstance(webViewFactoryProviderProxy);
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
			// 这里invokeResult的实际类型是
			// com.android.webview.chromium.WebViewChromium implements
			// WebViewProvider
			// 所以这里可以再次进行Proxy
			final WebView webView = (WebView) args[0];
			fixWebViewAsset(webView.getContext());
			return super.afterInvoke(target, method, args, beforeInvoke, invokeResult);
			// return ProxyUtil.createProxy(invokeResult, new MethodDelegate() {
			//
			// @Override
			// public Object beforeInvoke(Object target, Method method, Object[]
			// args) {
			// fixWebViewAsset(webView.getContext());
			// return super.beforeInvoke(target, method, args);
			// }
			//
			// });
		}
	}

	/**
	 * 这个方法是解决如下问题: 目前插件进程是多个插件共享的, 而Webview的全局Context是进程唯一的。
	 * 要让哪个插件能加载插件自己的Assets目录下的本地HTML, 就的将Webview的全局Context设置为哪个插件的AppContext
	 * 
	 * 但是当有多个插件在自己的Assets目录下的存在本地HTML文件时, Webview的全局Context无论设置为哪个插件的AppContext,
	 * 都会导致另外一个插件的Asest下的HTML文件加载不出来。
	 * 
	 * 因此每次切换Activity的时候都尝试将Webview的全局Context切换到当前Activity所在的AppContext
	 * 
	 * @param pluginActivity
	 */
	public static void switchWebViewContext(Context pluginActivity) {
		TwsLog.d(TAG, "尝试切换WebView Context, 不同的WebView内核, 实现方式可能不同, 本方法基于Chrome的WebView实现");
		try {
			/**
			 * webviewProvider获取过程： new WebView()
			 * ->WebViewFactory.getProvider().createWebView(this, new
			 * PrivateAccess()).init() ->loadChromiumProvider ->
			 * PathClassLoader("/system/framework/webviewchromium.jar")
			 * .forName(
			 * "com.android.webviewchromium.WebViewChromiumFactoryProvider")
			 * 
			 * -> BootLoader.forName(android.webkit.WebViewClassic$Factory)
			 * 
			 * ->new WebViewClassic.Factory()
			 */
			WebView wb = new WebView(pluginActivity);
			wb.loadUrl("");// 触发下面的fixWebViewAsset方法
		} catch (NullPointerException e) {
			e.printStackTrace();
			TwsLog.e(TAG, "插件Application对象尚未初始化会触发NPE，如果是异步初始化插件，应等待异步初始化完成再进入插件");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void fixWebViewAsset(Context context) {
		try {
			ClassLoader cl = null;
			if (sContextUtils == null) {
				if (sContentMain == null) {
					Object provider = HackWebViewFactory.getProvider();
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
								TwsLog.e(TAG, "触发了切换WebView Context");
							} catch (IllegalAccessException e) {
							} catch (IllegalArgumentException e) {
							} catch (InvocationTargetException e) {
							}
						}
					} catch (NoSuchMethodException ex) {
						try {
							// 不同的Chrome版本, 初始化的方法不同
							// for Chrome version 52
							sContextUtils = Class.forName("org.chromium.base.ContextUtils", true, cl);
						} catch (ClassNotFoundException e) {
						}
						if (sContextUtils != null) {
							RefInvoker.setField(null, sContextUtils, "sApplicationContext", null);
							RefInvoker.invokeMethod(null, sContextUtils, "initApplicationContext",
									new Class[] { Context.class }, new Object[] { context.getApplicationContext() });
							TwsLog.e(TAG, "触发了切换WebView Context");
						}
					}
				}
			} else {
				RefInvoker.setField(null, sContextUtils, "sApplicationContext", null);
				RefInvoker.invokeMethod(null, sContextUtils, "initApplicationContext", new Class[] { Context.class },
						new Object[] { context.getApplicationContext() });
				// 不同的Chrome版本, 初始化的方法不同
				// for Chrome version 52.0.2743.98
				RefInvoker.invokeMethod(null, sContextUtils, "initApplicationContextForNative", (Class[]) null,
						(Object[]) null);
				TwsLog.e(TAG, "触发了切换WebView Context");
			}

		} catch (Exception e) {
			TwsLog.e(TAG, "createWebview", e);
		}
	}

	private static Class sContentMain;
	private static Class sContextUtils;

}