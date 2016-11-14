package com.tws.plugin.core.android;

import android.util.SparseArray;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by cailiming on 16/10/30.
 */

public class HackAssetManager {

	private static final String ClassName = "android.content.res.AssetManager";

	private static final String Method_addAssetPath = "addAssetPath";
	private static final String Method_addAssetPaths = "addAssetPaths";

	private static final String Method_getAssignedPackageIdentifiers = "getAssignedPackageIdentifiers";

	private Object instance;

	public HackAssetManager(Object instance) {
		this.instance = instance;
	}

	public void addAssetPath(String path) {
		RefInvoker.invokeMethod(instance, ClassName, Method_addAssetPath, new Class[] { String.class },
				new Object[] { path });
	}

	public void addAssetPaths(String[] assetPaths) {
		RefInvoker.invokeMethod(instance, ClassName, Method_addAssetPaths, new Class[] { String[].class },
				new Object[] { assetPaths });

	}

	public SparseArray<String> getAssignedPackageIdentifiers() {
		SparseArray<String> packageIdentifiers = (SparseArray<String>) RefInvoker.invokeMethod(instance, ClassName,
				Method_getAssignedPackageIdentifiers, null, null);
		return packageIdentifiers;
	}
}
