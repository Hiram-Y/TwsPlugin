package com.example.plugindemo.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import tws.component.log.TwsLog;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.example.plugindemo.provider.PluginDbTables;
import com.example.plugindemo.receiver.PluginTestReceiver2;
import com.example.plugindemo.service.PluginTestService;
import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.sharelib.SharePOJO;
import com.tencent.tws.sharelib.util.HostProxy;

public class LauncherActivity extends Activity implements View.OnClickListener {

	private static final String TAG = "rick_Print:LauncherActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plugin_launcher);

		TwsLog.d("xxx1", "activity_welcome ID= " + R.layout.plugin_launcher);
		Log.e("xxx1", "activity_welcome ID= " + R.layout.plugin_launcher);
		Log.e("xxx2", getResources().getResourceEntryName(R.layout.plugin_launcher));
		TwsLog.d("xxx2", getResources().getResourceEntryName(R.layout.plugin_launcher));
		TwsLog.d(
				"xxx3",
				getResources().getString(R.string.app_name) + "  "
						+ getPackageManager().getApplicationLabel(getApplicationInfo()));
		TwsLog.d(
				"xxx3",
				getResources().getString(R.string.app_name) + "  "
						+ getPackageManager().getApplicationLabel(getApplicationInfo()));
		TwsLog.d("xxx4", getPackageName() + ", " + getText(R.string.app_name));
		TwsLog.d("xxx5", getResources().getString(android.R.string.httpErrorBadUrl));
		TwsLog.d("xxx6",
				getResources().getString(getResources().getIdentifier("app_name", "string", "com.example.plugindemo")));
		TwsLog.d("xxx7", getResources().getString(getResources().getIdentifier("app_name", "string", getPackageName())));
		// TwsLog.e("xxx8",
		// getResources().getString(getResources().getIdentifier("app_name",
		// "string", "com.example.pluginhost")));

//		ActionBar actionBar = getTwsActionBar();
//		actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_TITLE);
		setTitle("这是插件首屏");
		// if (actionBar == null) {
		// setTitle("这是插件首屏");
		// } else {
		// actionBar.setTitle("这是插件首屏");
		// actionBar.setSubtitle("这是副标题");
		// actionBar.setLogo(R.drawable.ic_launcher);
		// actionBar.setIcon(R.drawable.ic_launcher);
		// actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
		// ActionBar.DISPLAY_HOME_AS_UP
		// | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
		// }

		findViewById(R.id.onClickHellowrld).setOnClickListener(this);
		findViewById(R.id.onClickWeiXin).setOnClickListener(this);
		if (Build.VERSION.SDK_INT >= 21) {// Build.VERSION_CODES.KITKAT
			findViewById(R.id.onClickFacebook).setOnClickListener(this);
		} else {
			findViewById(R.id.onClickFacebook).setVisibility(View.GONE);
		}
		findViewById(R.id.onClickPluginNormalFragment).setOnClickListener(this);
		findViewById(R.id.onClickPluginSpecFragment).setOnClickListener(this);
		findViewById(R.id.onClickPluginForDialogActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginForOppoAndVivoActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginNotInManifestActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginFragmentTestActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginSingleTaskActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestOpenPluginActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestTabActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginWebViewActivity).setOnClickListener(this);
		findViewById(R.id.onClickTransparentActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestReceiver).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestReceiver2).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestService).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestService2).setOnClickListener(this);
		findViewById(R.id.db_insert).setOnClickListener(this);
		findViewById(R.id.db_read).setOnClickListener(this);
		findViewById(R.id.test_read_assert).setOnClickListener(this);
		findViewById(R.id.test_notification).setOnClickListener(this);
		findViewById(R.id.onClickSidebarActivity).setOnClickListener(this);
		findViewById(R.id.onClickTwsTActivity).setOnClickListener(this);
	}

	private static void startFragmentInHostActivity(Context context, String targetId) {
		Intent pluginActivity = new Intent();
		pluginActivity.setClassName(context, "com.example.pluginhost.TestFragmentActivity");
		pluginActivity.putExtra("PluginDispatcher.fragmentId", targetId);
		pluginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(pluginActivity);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.onClickHellowrld:
			onClickHellowrld(v);
			break;
		case R.id.onClickWeiXin:
			onClickWeiXin();
			break;
		case R.id.onClickFacebook:
			onClickFacebook();
			break;
		case R.id.onClickPluginNormalFragment:
			onClickPluginNormalFragment(v);
			break;
		case R.id.onClickPluginSpecFragment:
			onClickPluginSpecFragment(v);
			break;
		case R.id.onClickPluginForDialogActivity:
			onClickPluginForDialogActivity(v);
			break;
		case R.id.onClickPluginForOppoAndVivoActivity:
			onClickPluginForOppoAndVivoActivity(v);
			break;
		case R.id.onClickPluginNotInManifestActivity:
			onClickPluginNotInManifestActivity(v);
			break;
		case R.id.onClickPluginFragmentTestActivity:
			onClickPluginFragmentTestActivity(v);
			break;
		case R.id.onClickPluginSingleTaskActivity:
			onClickPluginSingleTaskActivity(v);
			break;
		case R.id.onClickPluginTestActivity:
			onClickPluginTestActivity(v);
			break;
		case R.id.onClickPluginTestOpenPluginActivity:
			onClickPluginTestOpenPluginActivity(v);
			break;
		case R.id.onClickPluginTestTabActivity:
			onClickPluginTestTabActivity(v);
			break;
		case R.id.onClickPluginWebViewActivity:
			onClickPluginWebViewActivity(v);
			break;
		case R.id.onClickTransparentActivity:
			onClickTransparentActivity(v);
			break;
		case R.id.onClickPluginTestReceiver:
			onClickPluginTestReceiver(v);
			break;
		case R.id.onClickPluginTestReceiver2:
			onClickPluginTestReceiver2(v);
			break;
		case R.id.onClickPluginTestService:
			onClickPluginTestService(v);
			break;
		case R.id.onClickPluginTestService2:
			onClickPluginTestService2(v);
			break;
		case R.id.db_insert:
			// 插件ContentProvider是在插件首次被唤起时安装的, 属于动态安装。
			// 因此需要在插件被唤起后才可以使用相应的ContentProvider
			// 若要静态安装，需要更改PluginLoader的安装策略～
			ContentValues values = new ContentValues();
			values.put(PluginDbTables.PluginFirstTable.MY_FIRST_PLUGIN_NAME, "test web" + System.currentTimeMillis());
			getContentResolver().insert(PluginDbTables.PluginFirstTable.CONTENT_URI, values);
			Toast.makeText(this, "ContentResolver insert test web", Toast.LENGTH_SHORT).show();
			break;
		case R.id.db_read:
			boolean isSuccess = false;
			Cursor cursor = getContentResolver().query(PluginDbTables.PluginFirstTable.CONTENT_URI, null, null, null,
					null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int index = cursor.getColumnIndex(PluginDbTables.PluginFirstTable.MY_FIRST_PLUGIN_NAME);
					if (index != -1) {
						isSuccess = true;
						String pluginName = cursor.getString(index);
						TwsLog.d(TAG, pluginName);
						Toast.makeText(this, "ContentResolver " + pluginName + " count=" + cursor.getCount(),
								Toast.LENGTH_SHORT).show();
					}
				}
				cursor.close();
			}
			if (!isSuccess) {
				Toast.makeText(this, "ContentResolver 查无数据", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.test_read_assert:
			testReadAssert();
			break;
		case R.id.test_notification:
			testNotification();
			break;
		case R.id.onClickSidebarActivity:
			onClickSidebarActivity();
			break;
		case R.id.onClickTwsTActivity:
			// 利用className打开共享控件的测试activity
			Intent intent = new Intent();
			intent.setClassName(this, TwsActivityDemo.class.getName());
			startActivity(intent);
			break;
		default:
			break;
		}
	}

	private void onClickHellowrld(View v) {
		Intent intent = getPackageManager().getLaunchIntentForPackage("com.example.pluginhelloworld");
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickWeiXin() {
		// 通过packageManager查询其他插件信息并打开,
		// 微信插件中没有配置launcher，所以这里假定用字符串“Send”来匹配
		PackageManager packageManager = getPackageManager();
		try {
			PackageInfo info = packageManager.getPackageInfo("com.example.wxsdklibrary", PackageManager.GET_ACTIVITIES);

			for (ActivityInfo activityInfo : info.activities) {
				if (activityInfo.name.contains("Send")) {
					Intent intent = new Intent();
					intent.setClassName(activityInfo.packageName, activityInfo.name);
					startActivity(intent);
					return;
				}
			}
			Toast.makeText(this, "TargetNotFound", Toast.LENGTH_SHORT).show();
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			Toast.makeText(this, "NameNotFoundException", Toast.LENGTH_SHORT).show();
		}
	}

	private void onClickFacebook() {
		// 通过packageManager查询其他插件信息并打开
		Intent intent = getPackageManager().getLaunchIntentForPackage("com.facebook.rebound.demo");
		startActivity(intent);
	}

	private void onClickPluginNormalFragment(View v) {
		startFragmentInHostActivity(this, "some_id_for_fragment1");
	}

	private void onClickPluginSpecFragment(View v) {
		startFragmentInHostActivity(this, "some_id_for_fragment2");
	}

	private void onClickPluginForDialogActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginForDialogActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickPluginForOppoAndVivoActivity(View v) {
		// 利用Action打开
		Intent intent = new Intent("test.ijk");
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickPluginNotInManifestActivity(View v) {
		// 利用scheme打开
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		intent.setData(Uri.parse("testscheme://testhost"));
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);

	}

	private void onClickPluginFragmentTestActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginFragmentTestActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickPluginSingleTaskActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginSingleTaskActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickPluginTestActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);

	}

	private void onClickPluginTestOpenPluginActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestOpenPluginActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickPluginTestTabActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestTabActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickPluginWebViewActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginWebViewActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickTransparentActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, TransparentActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickSidebarActivity() {
		// 利用className打开共享控件的测试activity
		Intent intent = new Intent();
		intent.setClassName(this, SideBarActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	private void onClickPluginTestReceiver(View v) {
		// 利用Action打开
		Intent intent = new Intent("test.rst2");// 两个Receive都配置了这个aciton，这里可以同时唤起两个Receiver
		intent.putExtra("testParam", "testParam");
		sendBroadcast(intent);
	}

	private void onClickPluginTestReceiver2(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestReceiver2.class.getName());
		intent.putExtra("testParam", "testParam");
		sendBroadcast(intent);
	}

	private void onClickPluginTestService(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestService.class.getName());
		intent.putExtra("testParam", "testParam");
		startService(intent);
		// stopService(intent);
	}

	private void onClickPluginTestService2(View v) {
		// 利用Action打开
		Intent intent = new Intent("test.lmn2");
		intent.putExtra("testParam", "testParam");
		startService(intent);
		// stopService(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("cc");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onResume() {
		super.onResume();
		testDataApi();
	}

	private void testDataApi() {

		SharedPreferences sp = getSharedPreferences("aaa", 0);
		sp.edit().putString("xyz", "123").commit();
		File f = getDir("bbb", 0);
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		f = getFilesDir();
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		// if (Build.VERSION.SDK_INT >= 21) {
		// f = getNoBackupFilesDir();
		// TwsLog.d(TAG, f.getAbsoluteFile() + " exists:" + f.exists() +
		// " canRead:" + f.canRead() + " canWrite:" + f.canWrite());
		// }

		f = getCacheDir();
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		// if (Build.VERSION.SDK_INT >= 21) {
		// f = getCodeCacheDir();
		// }
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		SQLiteDatabase db = openOrCreateDatabase("ccc", 0, null);
		try {
			String sql = "create table IF NOT EXISTS  userDb (_id integer primary key autoincrement, column_one text not null);";
			db.execSQL(sql);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}

		f = getDatabasePath("ccc");
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		String[] list = databaseList();

		try {
			FileOutputStream fo = openFileOutput("ddd", 0);
			fo.write(122);
			fo.flush();
			fo.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		TwsLog.d(TAG, getFileStreamPath("eee").getAbsolutePath());

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		TwsLog.d(TAG, "onKeyDown keyCode=" + keyCode);
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		TwsLog.d(TAG, "onKeyUp keyCode=" + keyCode);
		return super.onKeyUp(keyCode, event);
	}

	private void testReadAssert() {
		try {
			InputStream assestInput = getAssets().open("test.json");
			String text = streamToString(assestInput);
			Toast.makeText(this, "read assets from plugin" + text, Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void testNotification() {
		HostProxy.notification(LauncherActivity.class.getName(), "这是来自通知栏的参数", "来自插件ContentTitle", "来自插件ContentText");
	}

	private static String streamToString(InputStream input) throws IOException {

		InputStreamReader isr = new InputStreamReader(input);
		BufferedReader reader = new BufferedReader(isr);

		String line;
		StringBuffer sb = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		isr.close();
		return sb.toString();
	}
}