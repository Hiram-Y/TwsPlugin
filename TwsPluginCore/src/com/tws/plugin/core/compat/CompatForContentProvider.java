package com.tws.plugin.core.compat;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.util.RefInvoker;

/**
 * @author yongchen
 */
public class CompatForContentProvider {

    public static Bundle call(Uri uri, String method, String arg, Bundle extras) {

        ContentResolver resolver = PluginLoader.getApplication().getContentResolver();

        if (Build.VERSION.SDK_INT >= 11) {
            return resolver.call(uri, method, arg, extras);
        } else {
            ContentProviderClient client = resolver.acquireContentProviderClient(uri);
            if (client == null) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            try {
                Object mContentProvider = RefInvoker.getFieldObject(client, ContentProviderClient.class, "mContentProvider");
                if (mContentProvider != null) {
                    //public Bundle call(String method, String request, Bundle args)
                    Object result = RefInvoker.invokeMethod(mContentProvider, "android.content.IContentProvider", "call",
                            new Class[]{String.class, String.class, Bundle.class},
                            new Object[]{method, arg, extras});
                    return  (Bundle) result;
                }

            } finally {
                client.release();
            }
            return null;
        }
    }
}
