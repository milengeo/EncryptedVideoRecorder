package com.rustero.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.rustero.App;
import com.rustero.R;
import com.rustero.core.coders.EnfilmA;
import com.rustero.core.coders.Enfilm_1;
import com.rustero.core.effects.glEffect;
import com.rustero.core.stickers.StickerManager;
import com.rustero.core.stickers.StickerObject;
import com.rustero.gadgets.Mycams;
import com.rustero.gadgets.Size2D;
import com.rustero.gadgets.Tools;
import com.rustero.widgets.MyActivity;
import com.rustero.widgets.PanelManager;
import com.rustero.widgets.ToggleImageButton;

import java.util.ArrayList;
import java.util.List;




public class RecordActivity extends MyActivity {

	private final int LARGE_KNOB_SIZE = 120;
	private final int IMMERSE_SECS = 3;

	private ViewGroup mControlLayout;
    private ImageButton mBeginButton, mCeaseButton, mBackgroundButton, mSwitchButton;

    private ToggleImageButton mGridButton, mFlashButton;
    private TextView mFileName, mResoInfo, mZoomInfo, mTimeInfo, mHoriInfo;
    private ProgressBar mLoadingBar;

    private SurfaceView mSurfView;
	private SurfaceHolder mSurfHolder;
	private int mSurfWidth, mSurfHeight;

    private ScaleGestureDetector mScaleDetector;
    private Handler mTimerHandler;
    private int mTimerCount = 0;
	private int mImmerseSecs = 0;


	private PanelManager mPanelManager;
	private View mPanelBack;

	private ImageButton mOpenEffectPanel, mCloseEffectPanel;
	private View mEffectPanel;
	private ListView mEffectPanelList;
	private EffecstPanelAdapter mEffectsAdapter;
	private ArrayList<EffectItem> mEffectPanelItems;
	private TextView mSelectedEffects;

	private ImageButton mOpenThemePanel, mCloseThemePanel;
	private View mStickerPanel;
	private ListView mStickerPanelList;
	private StickerPanelAdapter mStickerAdapter;
	private ArrayList<StickerItem> mStickerPanelItems;




	public class EffectItem {
		public int tag, icon;
		public String name;

		// Constructor.
		public EffectItem(int tag, int icon, String name) {
			this.tag = tag;
			this.icon = icon;
			this.name = name;
		}
	}


	public class StickerItem {
		public int tag, icon;
		public String name;

		// Constructor.
		public StickerItem(int tag, int icon, String name) {
			this.tag = tag;
			this.icon = icon;
			this.name = name;
		}
	}





	@Override
	protected void onResume() {
		super.onResume();
		RecordService.activity = this;
		mSurfView.getHolder().addCallback(new SurfaceEventer());

		startRecordService();

		Enfilm_1.get().resetStatus();
		updateUI();
	}



	@Override
	protected void onPause() {
		super.onPause();
		RecordService.activity = null;

		if (isFinishing()) {
			stopRecordService();
		}

//		if (App.wantInter(this)) {
//			interstitialAd.show();
//			//App.showLongToast("Please click the ad to support the development");
//		}
	}



	@Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.record_activity);

			//loadInterstitialAd();
			mControlLayout = (ViewGroup) findViewById(R.id.record_layout_controls);
            mBeginButton = (ImageButton) findViewById(R.id.record_pubu_start);
            mBeginButton.setOnClickListener(new BeginClicker());
            App.screenScaled(mBeginButton, LARGE_KNOB_SIZE);

            mCeaseButton = (ImageButton) findViewById(R.id.record_pubu_stop);
            mCeaseButton.setOnClickListener(new CeaseClicker());
			App.screenScaled(mCeaseButton, LARGE_KNOB_SIZE);

			mBackgroundButton = (ImageButton) findViewById(R.id.record_pubu_background);
			mBackgroundButton.setOnClickListener(new BackgroundClicker());
			App.screenScaled(mBackgroundButton, 80);

			mGridButton = (ToggleImageButton) findViewById(R.id.record_btn_grid);
			mGridButton.setOnCheckedChangeListener(new GridClicker());
			App.screenScaled(mGridButton, 80);
			mGridButton.setChecked( App.getPrefBln("show_grid") );

			mFlashButton = (ToggleImageButton) findViewById(R.id.record_btn_flash);
			mFlashButton.setOnCheckedChangeListener(new FlashClicker());
			App.screenScaled(mFlashButton, 80);

			mSwitchButton = (ImageButton) findViewById(R.id.record_pubu_switch);
            mSwitchButton.setOnClickListener(new SwitchClicker());
			App.screenScaled(mSwitchButton, 80);

			mFileName = (TextView) findViewById(R.id.record_tevi_file_name);
			mHoriInfo = (TextView) findViewById(R.id.record_tevi_hori_info);
            mZoomInfo = (TextView) findViewById(R.id.record_tevi_zoom_info);
            mTimeInfo = (TextView) findViewById(R.id.record_tevi_time_info);
            mResoInfo = (TextView) findViewById(R.id.record_tevi_reso_info);
			mLoadingBar = (ProgressBar) findViewById(R.id.record_loading_bar);
            mSurfView = (SurfaceView) findViewById(R.id.id_port_view);
            mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());

			mPanelBack = findViewById(R.id.record_panel_back);
			mPanelManager = new PanelManager(222, mPanelBack, null);
			mEffectPanel = findViewById(R.id.panel_effects);

			mOpenEffectPanel = (ImageButton) findViewById(R.id.record_open_effect_panel);
			mOpenEffectPanel.setOnClickListener(OpenEffectClicker);
			App.screenScaled(mOpenEffectPanel, LARGE_KNOB_SIZE);

			mCloseEffectPanel = (ImageButton) findViewById(R.id.record_close_effect_panel);
			mCloseEffectPanel.setOnClickListener(ClosePanelClicker);

			mSelectedEffects = (TextView) findViewById(R.id.effects_panel_count);
			mEffectPanelItems = new ArrayList<EffectItem>();
			loadEffectList();

			mEffectPanelList = (ListView) findViewById(R.id.effects_panel_list);
			mEffectsAdapter = new EffecstPanelAdapter(this, R.layout.effect_row, mEffectPanelItems);
			mEffectPanelList.setAdapter(mEffectsAdapter);
			mEffectPanelList.setOnItemClickListener(new EffectItemClicker());

			mOpenThemePanel = (ImageButton) findViewById(R.id.record_open_sticker_panel);
			mOpenThemePanel.setOnClickListener(OpenThemeClicker);
			App.screenScaled(mOpenThemePanel, LARGE_KNOB_SIZE);

			mCloseThemePanel = (ImageButton) findViewById(R.id.record_close_sticker_panel);
			mCloseThemePanel.setOnClickListener(ClosePanelClicker);

			mStickerPanel = findViewById(R.id.theme_panel);
			mStickerPanelItems = new ArrayList<StickerItem>();
			loadStickerList();
			mStickerAdapter = new StickerPanelAdapter(this, R.layout.theme_row, mStickerPanelItems);
			mStickerPanelList = (ListView) findViewById(R.id.theme_panel_list);
			mStickerPanelList.setAdapter(mStickerAdapter);
			mStickerPanelList.setOnItemClickListener(new StickerItemClicker());

			Enfilm_1.create();
            App.gRecordedPath = "";

            if (Mycams.getCameraCount() > 1)
                mSwitchButton.setVisibility(View.INVISIBLE);

			mTimerHandler = new Handler();
            mTimerHandler.postDelayed(timerTack, 999);
        } catch (Exception ex) {
            App.log( " ***_ex RecordActivity_onCreate: " + ex.getMessage());
        };
    }



	@Override
	protected void onDestroy() {
//		if (interstitialAd != null) {
//			interstitialAd.destroy();
//		}
		super.onDestroy();
	}


//
//	private void loadInterstitialAd() {
//		if (App.IS_DEVEL)
//			interstitialAd = new InterstitialAd(this, "YOUR_PLACEMENT_ID");
//		else
//			interstitialAd = new InterstitialAd(this, "1842497116038581_1843949065893386");
//		interstitialAd.setAdListener(MyInterstitialAdListener);
//		interstitialAd.loadAd();
//	}


//
//	InterstitialAdListener MyInterstitialAdListener = new InterstitialAdListener() {
//		@Override
//		public void onInterstitialDisplayed(Ad ad) {
//			App.log("Interstitial displayed callback");
//		}
//
//		@Override
//		public void onInterstitialDismissed(Ad ad) {
//			App.log("Interstitial dismissed callback");
//		}
//
//		@Override
//		public void onError(Ad ad, AdError adError) {
//			App.log("interstitialAd Error: " + adError.getErrorMessage());
//		}
//
//		@Override
//		public void onAdLoaded(Ad ad) {
//			App.log("Interstitial onAdLoaded");
//		}
//
//		@Override
//		public void onAdClicked(Ad ad) {
//			App.log("Ad clicked callback");
//		}
//	};




	private void turnImmersive2(final boolean aOn) {
		mControlLayout.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (aOn) {
					getWindow().getDecorView().setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE
					);
					//App.log( "turnImmersive - On");

				} else {
					getWindow().getDecorView().setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_FULLSCREEN
					);
					//App.log("turnImmersive - Off");
					mImmerseSecs = 0;
				}
			}
		}, 555);
	}



	private Runnable timerTack = new Runnable() {
        @Override
        public void run() {
            mTimerCount++;
            //App.showShortToast("timerTack: " + mTimerCount);
            if (1 == mTimerCount) firstTack();
            doImmerseTack();
            mTimerHandler.postDelayed(this, 999);
        }
    };



    private void firstTack() {
        //App.showShortToast("firstTack");
    }



    private void doImmerseTack() {
		mImmerseSecs++;
        if (mImmerseSecs == IMMERSE_SECS) {
			//App.log( "mImmerseSecs == IMMERSE_SECS");
            turnImmersive2(true);
        }
    }



    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //App.log( "dispatchTouchEvent");
        mScaleDetector.onTouchEvent(event);
		turnImmersive2(false);
        return super.dispatchTouchEvent(event);
    }






    public class ScaleListener extends  ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float mFactor = 1.0f;


        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mFactor = detector.getScaleFactor();
            App.log( "onScale: " + mFactor);
            float prev = detector.getPreviousSpan();
            float curr = detector.getCurrentSpan();
            if (curr > prev) {
                App.log( "onScale inc");
				Enfilm_1.get().incZoom();
            } else if (curr < prev) {
                App.log( "onScale dec");
                Enfilm_1.get().decZoom();
            }
            String zoin = Enfilm_1.get().getZoom() + "x";
            mZoomInfo.setText(zoin);
            App.log( "onScale: " + Enfilm_1.get().getZoom());
            return true;
        }



        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            //App.log( "onScaleBegin");
            return true;
        }



        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            //App.log( "onScaleEnd: " + Enfilm_1.get().getZoom());
        }

    }



	private void startRecordService() {
		Intent startIntent = new Intent(this, RecordService.class);
		startIntent.setAction(App.INTENT_START_SERVICE);
		startService(startIntent);
	}



	private void stopRecordService() {
		Intent stopIntent = new Intent(this, RecordService.class);
		stopIntent.setAction(App.INTENT_STOP_SERVICE);
		startService(stopIntent);
	}





	private class BeginClicker implements View.OnClickListener {
        public void onClick(View v) {
			String folder = App.getOutputFolder();
			if (!Tools.folderExists(folder)) {
				App.showAlert(RecordActivity.this, "Something is wrong!", "The output directory does not exist!");
				return;
			}

			Intent startIntent = new Intent(RecordActivity.this, RecordService.class);
			startIntent.setAction(App.INTENT_BEGIN_RECORDING);
			startService(startIntent);
        }
    }




	private class CeaseClicker implements View.OnClickListener {
        public void onClick(View v) {
			Intent startIntent = new Intent(RecordActivity.this, RecordService.class);
			startIntent.setAction(App.INTENT_CEASE_RECORDING);
			startService(startIntent);
        }
    }



	private class SwitchClicker implements View.OnClickListener {
        public void onClick(View v) {
			boolean nowFront = App.getPrefBln("now_front");
			nowFront = !nowFront;
			App.setPrefBln("now_front", nowFront);

			stopRecordService();
			startRecordService();
        }
    }



	private class BackgroundClicker implements View.OnClickListener {
		public void onClick(View v) {
			moveTaskToBack(true);
		}
	}



	public class GridClicker implements ToggleImageButton.OnCheckedChangeListener {
		public void onCheckedChanged(ToggleImageButton buttonView, boolean isChecked) {
			if (null == Enfilm_1.get()) return;
			Enfilm_1.get().setGrid(isChecked);
			App.setPrefBln("show_grid", isChecked);
		}
    }



	public class FlashClicker implements ToggleImageButton.OnCheckedChangeListener {
		public void onCheckedChanged(ToggleImageButton buttonView, boolean isChecked) {
			if (null == Enfilm_1.get()) return;
			if (isChecked)
				Enfilm_1.get().turnFlash(true);
			else
				Enfilm_1.get().turnFlash(false);
		}
	}






	private boolean hasFlash() {
		boolean result = Enfilm_1.get().hasFlash();
		return result;
	}



	public void updateUI() {
		if (Enfilm_1.get().isRecording()) {
			mBeginButton.setVisibility(View.INVISIBLE);
			mCeaseButton.setVisibility(View.VISIBLE);
			mBackgroundButton.setVisibility(View.VISIBLE);
			mSwitchButton.setVisibility(View.INVISIBLE);
		} else {
			mBeginButton.setVisibility(View.VISIBLE);
			mCeaseButton.setVisibility(View.INVISIBLE);
			mBackgroundButton.setVisibility(View.INVISIBLE);
			mSwitchButton.setVisibility(View.VISIBLE);
		}
		mFileName.setText(Enfilm_1.get().getOutputName());

		Size2D camSize = Enfilm_1.get().getCameraSize();
		mResoInfo.setText(camSize.x + "x" + camSize.y);

		if (hasFlash())
			mFlashButton.setVisibility(View.VISIBLE);
		else
			mFlashButton.setVisibility(View.INVISIBLE);

		updateStatus();
	}



	public void updateStatus() {
		EnfilmA.StatusC status = Enfilm_1.get().getStatus();

		if (Enfilm_1.get().isRecording()) {
			if (status.size == 0)
				mLoadingBar.setVisibility(View.VISIBLE);
			else
				mLoadingBar.setVisibility(View.INVISIBLE);
		}

		String text = String.format("%02d:%02d", status.secs/60, status.secs%60);
		mTimeInfo.setText(text);

		TextView tevi = (TextView) findViewById(R.id.record_tevi_size_info);
		tevi.setText(Tools.formatSize(status.size));
	}










	View.OnClickListener OpenEffectClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			if (null == v)
				mPanelManager.quick = true;
			mPanelManager.openLeft(mEffectPanel);
			updateEffectsCount();
		}
	};



	View.OnClickListener OpenThemeClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			if (null == v)
				mPanelManager.quick = true;
			mPanelManager.openLeft(mStickerPanel);
		}
	};



	View.OnClickListener ClosePanelClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			mPanelManager.clear();
		}
	};




	private void loadEffectList() {
		List<String> list = glEffect.getEffects();
		for (String name : list) {
			mEffectPanelItems.add(new EffectItem(1, R.drawable.settings_54, name));
		}
	}



	private void updateEffectsCount() {
		mEffectsAdapter.notifyDataSetChanged();
		mSelectedEffects.setText("Selected: " + App.sMyEffects.size());
	}



	private class EffectItemClicker implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			EffectItem item = (EffectItem) view.getTag();
			if (null == item) return;
			boolean have = App.sMyEffects.contains(item.name);
			if (have)
				App.sMyEffects.remove(item.name);
			else
				App.sMyEffects.add(item.name);

			Enfilm_1.get().setEffects(App.sMyEffects);
			updateEffectsCount();
		}
	}






	public class EffecstPanelAdapter extends ArrayAdapter<EffectItem> {

		Context mContext;
		int layoutResourceId;
		ArrayList<EffectItem> mEffectList = null;

		public EffecstPanelAdapter(Context mContext, int layoutResourceId, ArrayList<EffectItem> data) {
			super(mContext, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.mContext = mContext;
			this.mEffectList = data;
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View itemView = convertView;
			if (itemView == null) {
				LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
				itemView = inflater.inflate(layoutResourceId, parent, false);
			}

			EffectItem item = mEffectList.get(position);
			itemView.setTag(item);

			TextView textViewName = (TextView) itemView.findViewById(R.id.effect_row_name);
			textViewName.setText(item.name);

			CheckBox cb = (CheckBox) itemView.findViewById(R.id.effect_row_check);
			if (App.sMyEffects.contains(item.name))
				cb.setChecked(true);
			else
				cb.setChecked(false);

			return itemView;
		}
	}





	private void loadStickerList() {
		List<String> list = StickerManager.get().getNames();
		for (String name : list) {
			mStickerPanelItems.add(new StickerItem(1, R.drawable.settings_54, name));
		}
	}




	private class StickerItemClicker implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			StickerItem item = (StickerItem) view.getTag();
			if (null == item) return;
			boolean selected = App.sMyTheme.equals(item.name);
			if (selected)
				App.sMyTheme = "";
			else
				App.sMyTheme = item.name;
			StickerObject theme = StickerManager.get().getTheme(App.sMyTheme);

			Enfilm_1.get().setTheme(theme);
			mStickerAdapter.notifyDataSetChanged();
		}
	}




	public class StickerPanelAdapter extends ArrayAdapter<StickerItem> {
		Context mContext;
		int layoutResourceId;
		ArrayList<StickerItem> mThemeList = null;

		public StickerPanelAdapter(Context mContext, int layoutResourceId, ArrayList<StickerItem> data) {
			super(mContext, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.mContext = mContext;
			this.mThemeList = data;
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View itemView = convertView;
			if (itemView == null) {
				LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
				itemView = inflater.inflate(layoutResourceId, parent, false);
			}

			StickerItem item = mThemeList.get(position);
			itemView.setTag(item);

			TextView textViewName = (TextView) itemView.findViewById(R.id.theme_row_name);
			textViewName.setText(item.name);

			RadioButton rabu = (RadioButton) itemView.findViewById(R.id.theme_row_radio);
			if (App.sMyTheme.equals(item.name))
				rabu.setChecked(true);
			else
				rabu.setChecked(false);

			return itemView;
		}
	}






	// * SurfaceHolder.Callback



	private class SurfaceEventer implements  SurfaceHolder.Callback {


		@Override
		public void surfaceCreated(SurfaceHolder aHolder) {
			App.log( "surfaceCreated");
			mSurfHolder = aHolder;
			Enfilm_1.get().attachScreen(aHolder);
		}



		@Override
		public void surfaceChanged(SurfaceHolder aHolder, int format, int aWidth, int aHeight) {
			App.log( "surfaceChanged fmt=" + format + " size=" + aWidth + "x" + aHeight);
			mSurfWidth = aWidth;
			mSurfHeight = aHeight;
			Enfilm_1.get().changeScreen(aWidth, aHeight);
		}



		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			App.log( "Surface destroyed");
			mSurfHolder = null;
			mSurfWidth = 0;
			mSurfHeight = 0;
			Enfilm_1.get().detachScreen();
		}
	}



}
