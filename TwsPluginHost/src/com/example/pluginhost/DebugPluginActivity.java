package com.example.pluginhost;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import tws.component.log.TwsLog;
import android.app.TwsActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.example.pluginhost.widget.StatusButton;
import com.tencent.tws.sharelib.IMyAidlInterface;
import com.tencent.tws.sharelib.SharePOJO;
import com.tencent.tws.sharelib.ShareService;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.localservice.LocalServiceManager;
import com.tws.plugin.core.manager.PluginCallback;
import com.tws.plugin.core.manager.PluginManagerHelper;
import com.tws.plugin.util.FileUtil;
import com.tws.plugin.util.ResourceUtil;

public class DebugPluginActivity extends TwsActivity {

	private static final String TAG = "rick_Print:MainActivity";
	private ViewGroup mList;
	private ViewGroup mBuiltinPlugList;
	private ViewGroup mSdcardPluginList;
	boolean isInstalled = false;
	private HashMap<String, String> mBuildinMap = new HashMap<String, String>();
	private String mInnerSDCardPath = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		setTitle("Host-插件调试界面");

		// SD卡存放路径
		mInnerSDCardPath = getInnerSDCardPath() + "/plugins";
		File sdPlgusFile = new File(mInnerSDCardPath);
		if (!sdPlgusFile.isDirectory() || !sdPlgusFile.exists()) {
			sdPlgusFile.mkdirs();
		}
		initView();

		// 监听插件安装 安装新插件后刷新当前页面
		registerReceiver(pluginInstallEvent, new IntentFilter(PluginCallback.ACTION_PLUGIN_CHANGED));
	}

	private static final String ASSETS_PLUGS_DIR = "plugins";

	private void initView() {
		mList = (ViewGroup) findViewById(R.id.list);
		mBuiltinPlugList = (ViewGroup) findViewById(R.id.builtin_plug_list);
		mSdcardPluginList = (ViewGroup) findViewById(R.id.sdcard_plugin_list);

		showInstalledAll();
		showBuildinPluginList();
		showSdcardPluginList();
	}

	private String mTmpFileName = "";

	private void showSdcardPluginList() {
		mSdcardPluginList.removeAllViews();
		File file = new File(mInnerSDCardPath);
		File[] subFile = file.listFiles();
		if (subFile == null || subFile.length < 1) {
			Toast.makeText(this, "Inner SDCard Plugins Path empty~!", Toast.LENGTH_SHORT).show();
			findViewById(R.id.sdcard_plugin_text).setVisibility(View.GONE);
			return;
		} else {
			findViewById(R.id.sdcard_plugin_text).setVisibility(View.VISIBLE);
		}

		for (File plugin : subFile) {
			mTmpFileName = plugin.getName();
			if (!mTmpFileName.endsWith(".apk"))
				continue;

			final StatusButton button = new StatusButton(this);
			button.setPadding(15, 3, 3, 3);
			LayoutParams layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParam.topMargin = 3;
			layoutParam.bottomMargin = 3;
			layoutParam.gravity = Gravity.LEFT;
			mSdcardPluginList.addView(button, layoutParam);

			String fileNameWithoutFix = getApkFileName(mTmpFileName);
			int iStatus = StatusButton.UNINSTALL_PLUGIN;
			if (mBuildinMap.containsKey(fileNameWithoutFix)) {
				iStatus = StatusButton.INSTALLED_PLUGIN;
			}

			button.setStatus(iStatus);
			// pluginFile位插件的文件全名，便于点击获取插件的文件名
			button.setPluginLabel(mTmpFileName);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (view instanceof StatusButton) {
						final StatusButton buttonEx = (StatusButton) view;
						if (buttonEx.getStatus() == StatusButton.INSTALLED_PLUGIN) {
							String pluginLabel = (String) buttonEx.getPluginLabel();
							String pluginId = mBuildinMap.get(getApkFileName(pluginLabel));

							if (!TextUtils.isEmpty(pluginId)) {
								PluginManagerHelper.remove(pluginId);
								buttonEx.setStatus(StatusButton.UNINSTALL_PLUGIN);
							}
						} else {
							String apkName = (String) buttonEx.getPluginLabel();
							PluginLoader.getInstance().setDillPluginName(apkName);
							PluginManagerHelper.installPlugin(mInnerSDCardPath + "//" + apkName);
							buttonEx.setStatus(StatusButton.INSTALLED_PLUGIN);
						}
					}
				}
			});
		}
	}

	private void showBuildinPluginList() {
		mBuiltinPlugList.removeAllViews();
		String[] mBuildInPlugins = null;
		try {
			mBuildInPlugins = getAssets().list(ASSETS_PLUGS_DIR);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String pluginFile : mBuildInPlugins) {
			if (!pluginFile.endsWith(".apk"))
				continue;

			final StatusButton button = new StatusButton(this);
			button.setPadding(15, 3, 3, 3);
			LayoutParams layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParam.topMargin = 3;
			layoutParam.bottomMargin = 3;
			layoutParam.gravity = Gravity.LEFT;
			mBuiltinPlugList.addView(button, layoutParam);

			String fileNameWithoutFix = getApkFileName(pluginFile);
			int iStatus = StatusButton.UNINSTALL_PLUGIN;
			if (mBuildinMap.containsKey(fileNameWithoutFix)) {
				iStatus = StatusButton.INSTALLED_PLUGIN;
			}

			button.setStatus(iStatus);
			// pluginFile位插件的文件全名，便于点击获取插件的文件名
			button.setPluginLabel(pluginFile);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					TwsLog.d(TAG, "onClick:" + view);
					if (view instanceof StatusButton) {
						final StatusButton buttonEx = (StatusButton) view;
						if (buttonEx.getStatus() == StatusButton.INSTALLED_PLUGIN) {
							String pluginLabel = (String) buttonEx.getPluginLabel();
							String pluginId = mBuildinMap.get(getApkFileName(pluginLabel));

							if (!TextUtils.isEmpty(pluginId)) {
								PluginManagerHelper.remove(pluginId);
								buttonEx.setStatus(StatusButton.UNINSTALL_PLUGIN);
							}
						} else {
							PluginLoader.getInstance().setDillPluginName((String) buttonEx.getPluginLabel());
							PluginLoader.copyAndInstall(ASSETS_PLUGS_DIR + "/" + (String) buttonEx.getPluginLabel());
							buttonEx.setStatus(StatusButton.INSTALLED_PLUGIN);
						}
					}
				}
			});

		}
	}

	private String getApkFileName(String apkFile) {
		if (!apkFile.endsWith(".apk"))
			return null;

		return apkFile.substring(0, apkFile.length() - 4);
	}

	private void showInstalledAll() {
		mBuildinMap.clear();
		ViewGroup root = mList;
		root.removeAllViews();
		// 列出所有已经安装的插件
		Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
		Iterator<PluginDescriptor> itr = plugins.iterator();
		while (itr.hasNext()) {
			final PluginDescriptor pluginDescriptor = itr.next();
			Button button = new Button(this);
			button.setPadding(9, 0, 9, 0);
			LayoutParams layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParam.topMargin = 3;
			layoutParam.bottomMargin = 3;
			layoutParam.gravity = Gravity.LEFT;
			root.addView(button, layoutParam);

			TwsLog.d(TAG, "插件id：" + pluginDescriptor.getPackageName());
			String pluginLabel = ResourceUtil.getLabel(pluginDescriptor);
			mBuildinMap.put(pluginLabel, pluginDescriptor.getPackageName());
			button.setText("打开插件：" + pluginLabel + ", V" + pluginDescriptor.getVersion());
			button.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent launchIntent = getPackageManager().getLaunchIntentForPackage(
							pluginDescriptor.getPackageName());
					if (launchIntent == null) {
						Toast.makeText(DebugPluginActivity.this,
								"插件" + pluginDescriptor.getPackageName() + "没有配置Launcher", Toast.LENGTH_SHORT).show();
						// 没有找到Launcher，打开插件详情
						Intent intent = new Intent(DebugPluginActivity.this, DetailActivity.class);
						intent.putExtra("plugin_id", pluginDescriptor.getPackageName());
						startActivity(intent);
					} else {
						// 打开插件的Launcher界面
						if (!pluginDescriptor.isStandalone()) {
							// 测试向非独立插件传宿主中定义的VO对象
							launchIntent.putExtra("paramVO", new SharePOJO("宿主传过来的测试VO"));
						}
						startActivity(launchIntent);
					}
				}
			});
		}

		if (!mBuildinMap.containsKey("TwsPluginDemo"))
			return;

		Button button = new Button(this);
		button.setPadding(9, 0, 9, 0);
		LayoutParams layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParam.topMargin = 3;
		layoutParam.bottomMargin = 3;
		layoutParam.gravity = Gravity.LEFT;
		root.addView(button, layoutParam);
		button.setText("打开控件级插件测试");
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(DebugPluginActivity.this, TestViewActivity.class);
				startActivity(intent);
			}
		});

		button = new Button(this);
		button.setPadding(9, 0, 9, 0);
		layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParam.topMargin = 3;
		layoutParam.bottomMargin = 3;
		layoutParam.gravity = Gravity.LEFT;
		root.addView(button, layoutParam);
		button.setText("唤起插件receiver");

		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent("test.rst2");// 两个Receive都配置了这个aciton，这里可以同时唤起两个Receiver
				intent.putExtra("testParam", "testParam");
				sendBroadcast(intent);
			}
		});

		// 测试通过宿主service唤起插件service
		button = new Button(this);
		button.setPadding(9, 0, 9, 0);
		layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParam.topMargin = 3;
		layoutParam.bottomMargin = 3;
		layoutParam.gravity = Gravity.LEFT;
		root.addView(button, layoutParam);
		button.setText("唤起插件service");

		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startService(new Intent(DebugPluginActivity.this, MainService.class));
			}
		});

		button = new Button(this);
		button.setPadding(9, 0, 9, 0);
		layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParam.topMargin = 3;
		layoutParam.bottomMargin = 3;
		layoutParam.gravity = Gravity.LEFT;
		root.addView(button, layoutParam);
		button.setText("测试插件Service AIDL");
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scn == null) {
					scn = new ServiceConnection() {
						@Override
						public void onServiceConnected(ComponentName name, IBinder service) {
							IMyAidlInterface iMyAidlInterface = IMyAidlInterface.Stub.asInterface(service);
							try {
								iMyAidlInterface.basicTypes(1, 2L, true, 0.1f, 0.01d, "测试插件AIDL");
								Toast.makeText(DebugPluginActivity.this, "onServiceConnected", Toast.LENGTH_SHORT)
										.show();
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void onServiceDisconnected(ComponentName name) {

						}
					};
				}
				bindService(new Intent("test.lmn"), scn, Context.BIND_AUTO_CREATE);
				// 顺便测试一下localservice的跨进程效果,这个在AMF.xml里面配置收录的，当前暂略
				ShareService ss = (ShareService) LocalServiceManager.getService("share_service");
				if (ss != null) {
					SharePOJO pojo = ss.doSomething("测试跨进程localservice");
					Toast.makeText(DebugPluginActivity.this, pojo.name, Toast.LENGTH_SHORT).show();
				}
			}
		});

		button = new Button(this);
		button.setPadding(9, 0, 9, 0);
		layoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParam.topMargin = 3;
		layoutParam.bottomMargin = 3;
		layoutParam.gravity = Gravity.LEFT;
		root.addView(button, layoutParam);
		button.setText("测试宿主tabActiviyt内嵌插件Activity");
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(DebugPluginActivity.this, TestTabActivity.class);
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(pluginInstallEvent);

		if (scn != null) {
			unbindService(scn);
			scn = null;
		}
	};

	private ServiceConnection scn;

	private final BroadcastReceiver pluginInstallEvent = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			showInstalledAll();
			showBuildinPluginList();
			showSdcardPluginList();
		};
	};

	@Override
	protected void onResume() {
		super.onResume();

		// 打印一下目录结构
		FileUtil.printAll(new File(getApplicationInfo().dataDir));
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

	/**
	 * 获取内置SD卡路径
	 * 
	 * @return
	 */
	public String getInnerSDCardPath() {
		return Environment.getExternalStorageDirectory().getPath();
	}
}
