package com.tws.plugin.core.compat;

import android.view.View;

import com.tws.plugin.util.RefInvoker;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * for supportv7
 */
public class CompatForSupportv7ViewInflater {

    private static final String android_support_v7_app_AppCompatViewInflater_sConstructorMap = "sConstructorMap";

    public static void installPluginCustomViewConstructorCache() {
        Map cache = (Map) RefInvoker.getFieldObject(null, "android.support.v7.app.AppCompatViewInflater", android_support_v7_app_AppCompatViewInflater_sConstructorMap);
        if (cache != null) {
            ConstructorHashMap<String, Constructor<? extends View>> newCacheMap = new ConstructorHashMap<String, Constructor<? extends View>>();
            newCacheMap.putAll(cache);
            RefInvoker.setFieldObject(null, "android.support.v7.app.AppCompatViewInflater", android_support_v7_app_AppCompatViewInflater_sConstructorMap, newCacheMap);
        }
    }

    public static class ConstructorHashMap<K, V> extends HashMap<K, V> {

        @Override
        public V put(K key, V value) {
            //不缓存
            return super.put(key, null);
        }

    }

}
