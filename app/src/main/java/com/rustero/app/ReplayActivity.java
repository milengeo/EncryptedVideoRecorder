
package com.rustero.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.rustero.App;
import com.rustero.Errors;
import com.rustero.R;
import com.rustero.core.coders.DefilmA;
import com.rustero.core.coders.Defilm_1;
import com.rustero.savi2api.Desavi;
import com.rustero.gadgets.Tools;
import com.rustero.widgets.MyActivity;



@SuppressWarnings("deprecation")




public class ReplayActivity extends MyActivity {

    private final int FADE_MILS = 555;
    private final int HIDE_SECS = 5;

	private static Desavi sDesavi;
    private ViewGroup mTopLayout;
    private ViewGroup mControlLayout;
	private ImageButton mPlayButton, mPauseButton;
    private TextView tvPosi, tvDura, tvError;
    private SeekBar sbSeeker;
    private boolean mDragging;

    private SurfaceView mSurfview;
//	private SurfaceHolder mSurfHolder;
	private int mSurfWidth, mSurfHeight;
//    private int mTimerCount = 0;
    private int mImmerseSecs = 0;
    private boolean mControlsHidden = false;
    private Handler mTimerHandler;
	private ProgressDialog mBufDlg = null;







	@Override
	protected void onResume() {
		super.onResume();
		//App.log( "onResume_99");
		mSurfview.getHolder().addCallback(new SurfaceEventer());
		launchDefilming();
		DefilmA.sEventer = new ReplayEventer();
	}



	@Override
	protected void onPause() {
		super.onPause();
		App.log("onPause_99");

		if (!isChangingConfigurations()) {
			doCease();
			finish();
		}

		DefilmA.sEventer = null;
	}






	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.replay_activity);

        mSurfview = (SurfaceView) findViewById(R.id.id_surface_view);
        mTopLayout = (ViewGroup) findViewById(R.id.play_layout_top);
        mControlLayout = (ViewGroup) findViewById(R.id.play_controls);

        mPlayButton = (ImageButton) findViewById(R.id.player_button_play);
        mPlayButton.setOnClickListener(new PlayClicker());

        mPauseButton = (ImageButton) findViewById(R.id.player_button_pause);
        mPauseButton.setOnClickListener(new PauseClicker());

        tvPosi = (TextView) findViewById(R.id.play_tevi_posi);
        tvDura = (TextView) findViewById(R.id.play_tevi_dura);
        tvError = (TextView) findViewById(R.id.play_tevi_error);

        sbSeeker = (SeekBar) findViewById(R.id.play_seekbar);
        sbSeeker.setMax(1000);
        sbSeeker.setOnSeekBarChangeListener(new SeekListener());

        final GestureDetector mGesturor = new GestureDetector(this, new Gesturor());
        mTopLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGesturor.onTouchEvent(event);
                return true;
            }
        });

        mTimerHandler = new Handler();
        mTimerHandler.postDelayed(timerTack, 999);
        updateControls();
    }




	private Runnable timerTack = new Runnable() {
        @Override
        public void run() {
            //mTimerCount++;
            //App.showShortToast("timerTack: " + mTimerCount);
			tryToHideControls();
            mTimerHandler.postDelayed(this, 999);
        }
    };



	private void launchDefilming() {
		App.log("launchDefilming_11");
		if (null != sDesavi) return;
		sDesavi = new Desavi(new DesaviEventer());
		sDesavi.begin(App.gPlayPath, App.gPlayPass);
	}



	private void fulfilDefilming() {
		App.log("fulfilDefilming_11");

		// start the sample pulling
		int method = sDesavi.getMethod();
		DefilmA.create(method, sDesavi);
		if (null == DefilmA.get()) {
			if (null != sDesavi) {
				sDesavi.cease();
				sDesavi = null;
			}
			App.finishActivityAlert(this, "Error", "Unknown compression format");
			return;
		}

		DefilmA.get().attachScreen(mSurfview.getHolder());
		DefilmA.get().changeScreen(mSurfWidth, mSurfHeight);
		DefilmA.get().begin();
	}



	private void spongeDefilming() {
		App.gPlayPass = "";
		App.gRecordPass = "";
		finish();
	}



	public void showBufDlg(Context aContext, String aMessage) {
		mBufDlg = new ProgressDialog(aContext);
		mBufDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mBufDlg.setMessage(aMessage);
		mBufDlg.setCanceledOnTouchOutside(false);
		mBufDlg.setCancelable(false);

		mBufDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//App.log("Cancel buffering");

				mControlLayout.postDelayed(new Runnable() {
					public void run() {
						doCease();
						ReplayActivity.this.finish();
						}
					}, 555);

			}
		});

		mBufDlg.show();
		Window window = mBufDlg.getWindow();
		window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
	}



	public void hideBufDlg() {
		if (null == mBufDlg) return;
		mBufDlg.dismiss();
		mBufDlg = null;
	}



    private void tryToHideControls() {
        if (mControlsHidden) return;
        mImmerseSecs++;
        if (mImmerseSecs >= HIDE_SECS)
            hideControls();
    }



    private class PlayClicker implements View.OnClickListener {
        public void onClick(View v) {
            doPauseOff();
        }
    }



    private class PauseClicker implements View.OnClickListener {
        public void onClick(View v) {
            doPauseOn();
        }
    }



    public void doPauseOn() {
		if (null == DefilmA.get()) return;
		DefilmA.get().pause(true);
        updateControls();
		mImmerseSecs = 0;
    }



    public void doPauseOff() {
		if (null == DefilmA.get()) return;
		DefilmA.get().pause(false);
        updateControls();
		mImmerseSecs = 0;
	}



    private void doCease() {
		if (null != DefilmA.get()) {
			//App.log( "doCease_DefilmA");
			DefilmA.get().cease();
			DefilmA.delete();
		}

		if (null != sDesavi) {
			//App.log( "doCease_sDesavi");
			sDesavi.cease();
			sDesavi = null;
		}

		//App.log( "doCease_33");
		hideBufDlg();

		//App.log( "doCease_44");
        updateControls();
		App.log( "doCease_99");
    }



    private void showControls() {
        if (!mControlsHidden) return;

        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        uiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        uiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        uiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        mControlsHidden = false;
        mControlLayout.setAlpha(0f);
        mControlLayout.setVisibility(View.VISIBLE);
        mControlLayout.animate()
            .alpha(1f)
            .setDuration(FADE_MILS)
            .setListener(null);
    }



    private void hideControls() {
        if (mControlsHidden) return;

        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        mControlsHidden = true;
        mImmerseSecs = 0;
        mControlLayout.setAlpha(1f);
        mControlLayout.animate()
            .alpha(0f)
            .setDuration(FADE_MILS)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mControlLayout.setVisibility(View.GONE);
                }
            });
    }




    // Updates the on-screen controls to reflect the current state of the app.
    private void updateControls() {
        if (DefilmA.get() == null) return;
        showControls();

        if (!DefilmA.get().getLastFault().isEmpty()) {
            mControlLayout.setVisibility(View.GONE);
            tvError.setText(DefilmA.get().getLastFault());
            tvError.setVisibility(View.VISIBLE);
        } else {
            mControlLayout.setVisibility(View.VISIBLE);
            tvError.setVisibility(View.GONE);
            if (!DefilmA.get().isPaused()) {
                mPlayButton.setVisibility(View.GONE);
                mPauseButton.setVisibility(View.VISIBLE);
            } else {
                mPlayButton.setVisibility(View.VISIBLE);
                mPauseButton.setVisibility(View.GONE);
            }
        }
    }



    private void updateProgress() {
        if (null == DefilmA.get()) return;

        int posi = DefilmA.get().getPosiSecs();
        tvPosi.setText(Tools.formatDuration(posi));

        if (mDragging) return;
        int dura = DefilmA.get().getDuramils();
        tvDura.setText(Tools.formatDuration(dura));
        int prog = 0;
        if (dura > 0) {
            prog = 1000 * posi / dura;
            sbSeeker.setProgress(prog);
        }
    }




	private class ReplayEventer implements Defilm_1.PlayEvents {


        public void onFrameSize() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //updateAspect();
                }
            });
        }


        public void onDestroyed() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    doCease();
                }
            });
        }


        public void onProgress() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateProgress();
                }
            });
        }


		public void onStopped() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateProgress();
                    updateControls();
				}
			});
		}


        public void onFault() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateControls();
                }
            });
        }

   };




    private class SeekListener implements SeekBar.OnSeekBarChangeListener {

		private int mProgress;

        public void onStartTrackingTouch(SeekBar bar) {
            mDragging = true;
        }


        public void onProgressChanged(SeekBar bar, int aProgress, boolean fromuser) {
            if (!mDragging) return;
			mProgress = aProgress;
            mImmerseSecs = 0;
        }


        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
			if (null == DefilmA.get()) return;
			int dura = DefilmA.get().getDuramils();
			int nepo = (dura * mProgress) / 1000;
			DefilmA.get().seekSecs(nepo);

            updateProgress();
        }
    };




    class Gesturor extends GestureDetector.SimpleOnGestureListener {


        @Override
        public boolean onDown(MotionEvent e) {
            //App.showShortToast("OnDown Detected ...");
            if (mControlsHidden)
                showControls();
			else
				mImmerseSecs = 0;
            return true;
        }


        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            //App.showShortToast("Single Tap Detected ...");
            return true;
        }


        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //App.showShortToast("Double Tap Detected ...");
            return true;
        }

    }







	private class DesaviEventer implements Desavi.Eventer {

		public void onCreate(final int aResult) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (0 == aResult) {
						App.log("onCreate success");
						fulfilDefilming();
					} else {
						App.log(" * onCreate error: " + aResult);
						setResult(aResult);
						App.sErrorText = Errors.getText(aResult);
						spongeDefilming();
					}
				}
			});
		}


		public void onBuffer(final boolean aOn) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (aOn) {
						//Toast.makeText (sActivity, "ON", Toast.LENGTH_SHORT).show();
						if (null == sDesavi) return;  // left over
						if (null == mBufDlg) {
							showBufDlg(ReplayActivity.this, "   Buffering ...  ");
//						else {
							String text = "   Buffering ...  " + sDesavi.getHungryPercent() + "%";
							mBufDlg.setMessage(text);
						}
					} else {
						//Toast.makeText (sActivity, "off", Toast.LENGTH_SHORT).show();
						hideBufDlg();
					}
				}
			});
		}

	}






	// * SurfaceHolder.Callback

	private class SurfaceEventer implements SurfaceHolder.Callback {
		@Override
		public void surfaceCreated(SurfaceHolder aHolder) {
			App.log( "surfaceCreated");
			if (null != DefilmA.get())
				DefilmA.get().attachScreen(aHolder);
		}


		@Override
		public void surfaceChanged(SurfaceHolder aHolder, int format, int aWidth, int aHeight) {
			App.log( "surfaceChanged fmt=" + format + " size=" + aWidth + "x" + aHeight);
			mSurfWidth = aWidth;
			mSurfHeight = aHeight;
			if (null != DefilmA.get())
				DefilmA.get().changeScreen(aWidth, aHeight);
		}


		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			App.log( "Surface destroyed");
			mSurfWidth = 0;
			mSurfHeight = 0;
			if (null != DefilmA.get())
				DefilmA.get().detachScreen();
		}

	}



}