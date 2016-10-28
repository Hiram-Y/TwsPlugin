package com.example.plugindemo.activity;

import android.app.Activity;
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

import com.example.plugindemo.R;
import com.example.plugindemo.vo.ParamVO;
import com.tencent.tws.sharelib.util.HostProxy;

/**
 * 插件中引用主程序资源文件需要显式的指定引用的R 因为主程序的资源id每次编译时都会变化 所以使用主程序资源id的时候必须使用引用
 * 而不是id的const常量， 因此在插件工程code-gen时没有合并依赖库的R，（合并后会获得依赖库R的id常量）
 * 而是将依赖库的R文件作为编译时的classpath引用 反编译插件可以看到，插件资源id都替换成了常量，二非插件id还是保留R.id的引用形式
 * 这正是我们想要的结果
 * 
 * 不携带插件相关代码的activity。完全就是一个普通的activity
 * 
 * 
 * @author yongchen
 * 
 */
public class PluginTestActivity extends Activity implements OnClickListener {

	private ViewGroup mRoot;
	private LayoutInflater mInflater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("测试插件中的Activity");

		Log.d("xx", getApplication().toString() + "," + getApplicationContext().toString() + ","
				+ getApplication().getApplicationContext().toString());

		Log.d("xx", ((ParamVO) getIntent().getSerializableExtra("paramvo")) + ", action:" + getIntent().getAction()
				+ getIntent().toUri(0));

		mInflater = getLayoutInflater();
		View scrollview = mInflater.inflate(R.layout.plugin_layout, null);

		mRoot = (ViewGroup) scrollview.findViewById(R.id.content);

		initViews();

		setContentView(scrollview);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "test plugin menu");
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
		Log.v("v.click 111", "" + v.getId() + " " + getResources().getClass().getName());
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
