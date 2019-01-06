package com.rustero.widgets;


import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

import com.rustero.App;


public class MyActivity extends AppCompatActivity {


	protected long mResumeMils;
	protected Menu mMainMenu;



	protected void onResume() {
		super.onResume();
		App.setActivity(this);
		mResumeMils = App.mils();

	}


	protected void onPause() {
		super.onPause();
		App.setActivity(null);

		if (null != mMainMenu) {
			mMainMenu.close();
			mMainMenu = null;
		}
	}




	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mMainMenu = menu;
		return true;
	}


	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		mMainMenu = null;
	}



	public long getActSecs() {
		return (App.mils() - mResumeMils) / 1000;
	}

}
