package com.example.plugindemo.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;

import com.example.plugindemo.fragment.PluginNormalFragment;

/**
 * 
 * @author yongchen
 */
public class PluginFragmentTestActivity extends Activity {

	private static final String LOG_TAG = PluginFragmentTestActivity.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("测试插件中的FragmentActivity");
		FrameLayout root = new FrameLayout(this);
		setContentView(root, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		root.setId(android.R.id.primary);

		Fragment fragment = new PluginNormalFragment();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(android.R.id.primary, fragment).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "test plugin menu");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Toast.makeText(this, "test plugin menu", Toast.LENGTH_SHORT).show();
		Log.e(LOG_TAG, "" + item.getTitle());
		return super.onOptionsItemSelected(item);
	}

}
