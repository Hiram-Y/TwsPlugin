package com.example.plugindemo.fragment;

import tws.component.log.TwsLog;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.tencent.tws.sharelib.util.HostProxy;

/**
 * 此fragment没有使用特定的context,因此只能在插件中的activity，或者宿主中的特定activity中展示
 */
public class PluginNormalFragment extends Fragment implements OnClickListener {

	private static final String TAG = "rick_Print:";
	private ViewGroup mRoot;
	private LayoutInflater mInflater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		getActivity().setTitle("测试插件中的Fragment，使用插件默认主题");

		mInflater = inflater;
		View scrollview = mInflater.inflate(R.layout.plugin_layout, null);

		mRoot = (ViewGroup) scrollview.findViewById(R.id.content);

		initViews();

		return scrollview;
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
		TwsLog.d(TAG, "onClick");
		if (v.getId() == R.id.plugin_test_btn1) {
			View view = mInflater.inflate(R.layout.plugin_layout, null, false);
			mRoot.addView(view);
			Toast.makeText(this.getActivity(), getString(R.string.hello_world1), Toast.LENGTH_SHORT).show();
		} else if (v.getId() == R.id.plugin_test_btn2) {
			int shareLayoutID = HostProxy.getShareLayoutId("share_main");
			TwsLog.d(TAG, "get shareLayoutID=" + Integer.toHexString(shareLayoutID));
			View view = mInflater.inflate(shareLayoutID, null, false);
			TwsLog.d(TAG, "get inflate(shareLayoutID view=" + view);
			mRoot.addView(view);

			int shareStringid = HostProxy.getShareStringId("share_string_1");
			TwsLog.d(TAG, "get shareStringid=" + Integer.toHexString(shareStringid));
			Toast.makeText(this.getActivity(), getString(shareStringid), Toast.LENGTH_SHORT).show();
		} else if (v.getId() == R.id.plugin_test_btn3) {
			View view = LayoutInflater.from(getActivity()).inflate(HostProxy.getShareLayoutId("share_main"), null,
					false);
			mRoot.addView(view);
		} else if (v.getId() == R.id.plugin_test_btn4) {
			((Button) v).setText(HostProxy.getShareStringId("share_string_1"));
		}
	}
}
