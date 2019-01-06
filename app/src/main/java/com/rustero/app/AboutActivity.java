package com.rustero.app;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.rustero.App;
import com.rustero.R;
import com.rustero.widgets.MyActivity;


public class AboutActivity extends MyActivity {

    private Button btnVisit;
    private String mVername;

	@Override
	@SuppressWarnings("ConstantConditions")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}});

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVername = pInfo.versionName;
        }
        catch (Exception ex) {}

		TextView tvPremium = (TextView) findViewById(R.id.about_premium);
		if (App.isPremium())
			tvPremium.setVisibility(View.VISIBLE);
		else
			tvPremium.setVisibility(View.GONE);

        TextView tvVersion = (TextView) findViewById(R.id.tv_about_vername);
        tvVersion.setText(getResources().getText(R.string.version) + ": " + mVername);

        btnVisit = (Button)findViewById(R.id.about_btn_visit);
        btnVisit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://rustero.com/index.html#encrypted_video_recorder"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
            }
        });

    }




}
