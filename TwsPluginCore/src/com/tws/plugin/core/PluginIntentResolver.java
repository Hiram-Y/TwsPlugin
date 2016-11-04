package com.tws.plugin.core;

import java.util.ArrayList;

import tws.component.log.TwsLog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;

import com.tws.plugin.content.PluginActivityInfo;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.content.PluginReceiverIntent;
import com.tws.plugin.core.manager.PluginManagerHelper;
import com.tws.plugin.util.ProcessUtil;
import com.tws.plugin.util.RefInvoker;

public class PluginIntentResolver {
	private static final String TAG = "rick_Print:PluginIntentResolver";

	public static final String CLASS_SEPARATOR = "@";// 字符串越短,判断时效率越高
	public static final String CLASS_PREFIX_RECEIVER = "#";// 字符串越短,判断时效率越高
	public static final String CLASS_PREFIX_SERVICE = "%";// 字符串越短,判断时效率越高

	public static void resolveService(Intent intent) {
		ArrayList<String> classNameList = PluginLoader.matchPlugin(intent, PluginDescriptor.SERVICE);
		if (classNameList != null && classNameList.size() > 0) {
			String stubServiceName = PluginManagerHelper.bindStubService(classNameList.get(0));
			if (stubServiceName != null) {
				intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), stubServiceName));
			}
		} else {
			if (intent.getComponent() != null
					&& null != PluginManagerHelper
							.getPluginDescriptorByPluginId(intent.getComponent().getPackageName())) {
				intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), intent
						.getComponent().getClassName()));
			}
		}
	}

	public static ArrayList<Intent> resolveReceiver(final Intent intent) {
		// 如果在插件中发现了匹配intent的receiver项目，替换掉ClassLoader
		// 不需要在这里记录目标className，className将在Intent中传递
		ArrayList<Intent> result = new ArrayList<Intent>();
		ArrayList<String> classNameList = PluginLoader.matchPlugin(intent, PluginDescriptor.BROADCAST);
		if (classNameList != null && classNameList.size() > 0) {
			for (String className : classNameList) {
				Intent newIntent = new Intent(intent);
				newIntent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(),
						PluginManagerHelper.bindStubReceiver()));
				// hackReceiverForClassLoader检测到这个标记后会进行替换
				newIntent.setAction(className + CLASS_SEPARATOR
						+ (intent.getAction() == null ? "" : intent.getAction()));
				result.add(newIntent);
			}
		} else {
			if (intent.getComponent() != null
					&& null != PluginManagerHelper
							.getPluginDescriptorByPluginId(intent.getComponent().getPackageName())) {
				intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), intent
						.getComponent().getClassName()));
			}
		}

		// fix 插件中对同一个广播同时注册了动态和静态广播的情况
		result.add(intent);

		return result;
	}

	/* package */static Class resolveReceiverForClassLoader(Object msgObj) {
		Intent intent = (Intent) RefInvoker.getFieldObject(msgObj, "android.app.ActivityThread$ReceiverData", "intent");
		if (intent.getComponent().getClassName().equals(PluginManagerHelper.bindStubReceiver())) {
			String action = intent.getAction();
			TwsLog.d(TAG, "action:" + action);
			if (action != null) {
				String[] targetClassName = action.split(CLASS_SEPARATOR);
				@SuppressWarnings("rawtypes")
				Class clazz = PluginLoader.loadPluginClassByName(targetClassName[0]);
				if (clazz != null) {
					intent.setExtrasClassLoader(clazz.getClassLoader());
					// 由于之前intent被修改过 这里再吧Intent还原到原始的intent
					if (targetClassName.length > 1) {
						intent.setAction(targetClassName[1]);
					} else {
						intent.setAction(null);
					}
				}
				// PluginClassLoader检测到这个特殊标记后会进行替换
				intent.setComponent(new ComponentName(intent.getComponent().getPackageName(), CLASS_PREFIX_RECEIVER
						+ targetClassName[0]));

				if (Build.VERSION.SDK_INT >= 21) {
					if (intent.getExtras() != null) {
						PluginReceiverIntent newIntent = new PluginReceiverIntent(intent);
						RefInvoker.setFieldObject(msgObj, "android.app.ActivityThread$ReceiverData", "intent",
								newIntent);
					}
				}

				return clazz;
			}
		}
		return null;
	}

	/* package */static String resolveServiceForClassLoader(Object msgObj) {

		ServiceInfo info = (ServiceInfo) RefInvoker.getFieldObject(msgObj,
				"android.app.ActivityThread$CreateServiceData", "info");

		if (ProcessUtil.isPluginProcess()) {

			PluginInjector.hackHostClassLoaderIfNeeded();

			// 通过映射查找
			String targetClassName = PluginManagerHelper.getBindedPluginServiceName(info.name);
			// TODO 或许可以通过这个方式来处理service
			// info.applicationInfo = XXX

			TwsLog.d(TAG, "hackServiceName=" + info.name + " packageName=" + info.packageName + " processName="
					+ info.processName + " targetClassName=" + targetClassName + " applicationInfo.packageName="
					+ info.applicationInfo.packageName);

			if (targetClassName != null) {
				info.name = CLASS_PREFIX_SERVICE + targetClassName;
			} else if (PluginManagerHelper.isStub(info.name)) {
				String dumpString = PluginManagerHelper.dumpServiceInfo();
				TwsLog.e(TAG, "hackServiceName 没有找到映射关系, 可能映射表出了异常 info.name=" + info.name + " dumpString="
						+ dumpString);

				info.name = CLASS_PREFIX_SERVICE + "null";
			} else {
				TwsLog.d(TAG, "是宿主service:" + info.name);
			}
		}

		return info.name;
	}

	public static void resolveActivity(Intent intent) {
		// 如果在插件中发现Intent的匹配项，记下匹配的插件Activity的ClassName
		ArrayList<String> classNameList = PluginLoader.matchPlugin(intent, PluginDescriptor.ACTIVITY);
		if (classNameList != null && classNameList.size() > 0) {

			String className = classNameList.get(0);
			PluginDescriptor pd = PluginManagerHelper.getPluginDescriptorByClassName(className);

			PluginActivityInfo pluginActivityInfo = pd.getActivityInfos().get(className);

			String stubActivityName = PluginManagerHelper.bindStubActivity(className,
					Integer.parseInt(pluginActivityInfo.getLaunchMode()));

			intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), stubActivityName));
			// PluginInstrumentationWrapper检测到这个标记后会进行替换
			intent.setAction(className + CLASS_SEPARATOR + (intent.getAction() == null ? "" : intent.getAction()));
		} else {
			if (intent.getComponent() != null
					&& null != PluginManagerHelper
							.getPluginDescriptorByPluginId(intent.getComponent().getPackageName())) {
				intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), intent
						.getComponent().getClassName()));
			}
		}
	}

	/* package */static void resolveActivity(Intent[] intent) {
		// 不常用。需要时再实现此方法，
	}

}
