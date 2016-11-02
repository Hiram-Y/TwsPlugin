package com.example.plugindemo.activity;

import android.app.TwsActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.plugindemo.R;

public class TwsActivityDemo extends TwsActivity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tws_demo);

		findViewById(R.id.test_btn).setOnClickListener(this);
		
		getTwsActionBar().setTitle("1231231231");
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.test_btn:
			Toast.makeText(this, "onClick", Toast.LENGTH_SHORT).show();
			break;

		default:
			break;
		}
	}

}
