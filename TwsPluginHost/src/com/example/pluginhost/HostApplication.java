package com.example.pluginhost;

import android.content.Context;

import com.tencent.tws.sharelib.util.HostProxy;
import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.util.ProcessUtil;

public class HostApplication extends PluginApplication {
	@Override
	public void onCreate() {
		super.onCreate();

		// 可选, 指定loading页UI, 用于首次加载插件时, 显示菊花等待插件加载完毕,
		if (ProcessUtil.isPluginProcess(this)) {
			PluginLoader.setLoadingResId(R.layout.plugin_loading);
		}
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		HostProxy.setApplication(this);
	}
}
