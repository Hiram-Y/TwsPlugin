package com.example.plugindemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.plugindemo.PluginTestApplication;
import com.example.plugindemo.R;
import com.example.plugindemo.receiver.PluginTestReceiver;
import com.example.plugindemo.vo.ParamVO;
import com.tencent.tws.sharelib.util.HostProxy;

/**
 * 完整生命周期模式 不使用反射、也不使用代理，真真正证实现activity无需在Manifest中注册！
 * 
 * @author yongchen
 *
 */
public class PluginNotInManifestActivity extends Activity implements OnClickListener {

	private ViewGroup mRoot;
	private LayoutInflater mInflater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("测试插件中拥有真正生命周期的Activity");
		mInflater = getLayoutInflater();
		View scrollview = mInflater.inflate(R.layout.plugin_layout, null);

		mRoot = (ViewGroup) scrollview.findViewById(R.id.content);

		initViews();

		setContentView(scrollview);

		Toast.makeText(this, ""+ ((PluginTestApplication) getApplication()).getApplicationContext().toString(), Toast.LENGTH_SHORT).show();

		//测试动态注册的插件广播
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestReceiver.class.getName());
		intent.putExtra("str1", "打开PluginTestReceiver——————");
		ParamVO pvo = new ParamVO();
		pvo.name = "打开PluginTestReceiver";
		intent.putExtra("paramvo", pvo);
		getApplication().sendBroadcast(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0,0, 0, "test plugin menu");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Toast.makeText(this, "test plugin menu", Toast.LENGTH_SHORT).show();
		Log.e("xx", "" + item.getTitle());
		return super.onOptionsItemSelected(item);
	}
	
	public void initViews() {

		Button btn1 = (Button) mRoot.findViewById(R.id.plugin_test_btn1);
		btn1.setOnClickListener(this);

		Button btn2 = (Button) mRoot.findViewById(R.id.plugin_test_btn2);
		btn2.setOnClickListener(this);

		Button btn3 = (Button) mRoot.findViewById(R.id.plugin_test_btn3);
		btn3.setOnClickListener(this);

		Button btn4 = (Button) mRoot.findViewById(R.id.plugin_test_btn4);
		btn4.setOnClickListener(this);
 
	}

	@Override
	public void onClick(View v) {
		Log.v("v.click 111", "" + v.getId());
		if (v.getId() == R.id.plugin_test_btn1) {
			View view = mInflater.inflate(R.layout.plugin_layout, null, false);
			mRoot.addView(view);
			Toast.makeText(this, getString(R.string.hello_world1), Toast.LENGTH_SHORT).show();
		} else if (v.getId() == R.id.plugin_test_btn2) {
			View view = mInflater.inflate(HostProxy.getShareLayoutId("share_main"), null, false);
			mRoot.addView(view);
			Toast.makeText(this, getString(HostProxy.getShareStringId("share_string_1")), Toast.LENGTH_SHORT).show();
		} else if (v.getId() == R.id.plugin_test_btn3) {
			View view = LayoutInflater.from(this).inflate(HostProxy.getShareLayoutId("share_main"), null, false);
			mRoot.addView(view);
		} else if (v.getId() == R.id.plugin_test_btn4) {
			((Button) v).setText(HostProxy.getShareStringId("share_string_2"));
		}
	}

}
