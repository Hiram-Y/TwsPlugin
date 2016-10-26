package com.example.accesservice;

import tws.component.log.TwsLog;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

/**
 * @author yongchen
 */
public class TestAccessibilitySerivce extends AccessibilityService {

	private static final String TAG = "TestAccessibilitySerivce";

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.packageNames = new String[] { "com.example.pluginhost" }; // 监听过滤的包名
		info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK; // 监听哪些行为
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN; // 反馈
		info.notificationTimeout = 100; // 通知的时间
		setServiceInfo(info);
		TwsLog.d(TAG, "xxx onServiceConnected");
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		TwsLog.d(TAG, "xxx AccessibilityEvent : " + event.toString());
	}

	@Override
	public void onInterrupt() {
	}
}
