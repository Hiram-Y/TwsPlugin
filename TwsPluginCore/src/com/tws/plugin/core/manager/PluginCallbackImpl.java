package com.tws.plugin.core.manager;

import android.content.Intent;

import com.tws.plugin.core.PluginLoader;

/**
 * @author yongchen
 */
public class PluginCallbackImpl implements PluginCallback {

	private static final String extra_type = "type";
	private static final String extra_id = "id";
	private static final String extra_version = "version";
	private static final String extra_result_code = "code";
	private static final String extra_src = "src";

	@Override
	public void onInstall(int result, String packageName, String version, String src) {
		if (result == InstallResult.SUCCESS) {
			PluginLoader.pluginChangedCallBack(PluginLoader.PLUGIN_UPDATE_ADD, packageName);
		} else {
			String description = packageName + "install rlt:" + result;
			PluginLoader.pluginChangedCallBack(PluginLoader.PLUGIN_UPDATE_ERROR, description);
		}
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(extra_type, "install");
		intent.putExtra(extra_id, packageName);
		intent.putExtra(extra_version, version);
		intent.putExtra(extra_result_code, result);
		intent.putExtra(extra_src, src);
		PluginLoader.getApplication().sendBroadcast(intent);
	}

	@Override
	public void onRemove(String packageName, boolean success) {
		PluginLoader.pluginChangedCallBack(PluginLoader.PLUGIN_UPDATE_REMOVE, packageName);
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(extra_type, "remove");
		intent.putExtra(extra_id, packageName);
		intent.putExtra(extra_result_code, success ? 0 : 7);
		PluginLoader.getApplication().sendBroadcast(intent);
	}

	@Override
	public void onRemoveAll(boolean success) {
		PluginLoader.pluginChangedCallBack(PluginLoader.PLUGIN_UPDATE_REMOVEALL, "");
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(extra_type, "remove_all");
		intent.putExtra(extra_result_code, success ? 0 : 7);
		PluginLoader.getApplication().sendBroadcast(intent);
	}

	// 未使用
	@Override
	public void onStart(String packageName) {
		PluginLoader.pluginChangedCallBack(PluginLoader.PLUGIN_UPDATE_START, packageName);
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(extra_type, "start");
		intent.putExtra(extra_id, packageName);
		PluginLoader.getApplication().sendBroadcast(intent);
	}

	// 未使用
	@Override
	public void onStop(String packageName) {
		PluginLoader.pluginChangedCallBack(PluginLoader.PLUGIN_UPDATE_STOP, packageName);
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(extra_type, "stop");
		intent.putExtra(extra_id, packageName);
		PluginLoader.getApplication().sendBroadcast(intent);
	}
}
