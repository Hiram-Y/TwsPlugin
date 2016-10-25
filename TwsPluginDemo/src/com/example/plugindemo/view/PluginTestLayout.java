package com.example.plugindemo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.plugin.util.HostProxy;

/**
 * 仅仅用来测试插件程序找中是否可以使用自定义控件
 * @author yongchen
 *
 */
public class PluginTestLayout extends LinearLayout {
	
	public PluginTestLayout(Context context) {
		super(context);
	}
	
	public PluginTestLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		LayoutInflater.from(getContext()).inflate(HostProxy.getShareLayoutId("share_layout"), this);
	}
}
