package com.tws.plugin.core.compat;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import android.view.View;

import com.tws.plugin.util.RefInvoker;

/**
 * for supportv7
 */
public class CompatForSupportv7ViewInflater {

	private static final String android_support_v7_app_AppCompatViewInflater = "android.support.v7.app.AppCompatViewInflater";
	private static final String android_support_v7_app_AppCompatViewInflater_sConstructorMap = "sConstructorMap";

	public static void installPluginCustomViewConstructorCache() {
		Class AppCompatViewInflater = null;
		try {
			AppCompatViewInflater = Class.forName(android_support_v7_app_AppCompatViewInflater);
			Map cache = (Map) RefInvoker.getField(null, AppCompatViewInflater,
					android_support_v7_app_AppCompatViewInflater_sConstructorMap);
			if (cache != null) {
				ConstructorHashMap<String, Constructor<? extends View>> newCacheMap = new ConstructorHashMap<String, Constructor<? extends View>>();
				newCacheMap.putAll(cache);
				RefInvoker.setField(null, AppCompatViewInflater,
						android_support_v7_app_AppCompatViewInflater_sConstructorMap, newCacheMap);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static class ConstructorHashMap<K, V> extends HashMap<K, V> {

		@Override
		public V put(K key, V value) {
			// 不缓存
			return super.put(key, null);
		}

	}

}
