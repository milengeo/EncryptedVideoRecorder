
package com.rustero.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.rustero.App;
import com.rustero.Errors;
import com.rustero.R;
import com.rustero.core.coders.DefilmA;
import com.rustero.core.coders.Defilm_1;
import com.rustero.core.coders.Enmuxer;
import com.rustero.savi2api.Desavi;
import com.rustero.gadgets.Tools;
import com.rustero.widgets.MyActivity;

import java.io.File;
import java.nio.ByteBuffer;


@SuppressWarnings("deprecation")




public class ExportActivity extends MyActivity {

	public static int sLastResult;
	private Desavi mDesavi;
    private TextView tvPosi, tvDura, tvError, tvName;
    private SeekBar sbSeeker;

    private SurfaceView mSurfview;
//	private SurfaceHolder mSurfHolder;
	private int mSurfWidth, mSurfHeight;

	private DemuxEventer mDemuxEventer;
    private int mTimerCount = 0;
    private Handler mTimerHandler;
	private Enmuxer mEnmuxer;
	private ProgressDialog mBufDlg = null;






	@Override
	protected void onResume() {
		super.onResume();
		mSurfview.getHolder().addCallback(new SurfaceEventer());
		launchDefilming();
		DefilmA.sEventer = new ReplayEventer();
	}



	@Override
	protected void onPause() {
		super.onPause();
		DefilmA.sEventer = null;
		doCease();
	}



	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.export_activity);

        mSurfview = (SurfaceView) findViewById(R.id.export_surface_view);
        tvPosi = (TextView) findViewById(R.id.export_tevi_posi);
        tvDura = (TextView) findViewById(R.id.export_tevi_dura);
        tvError = (TextView) findViewById(R.id.export_tevi_error);
		tvName = (TextView) findViewById(R.id.export_tevi_name);

        sbSeeker = (SeekBar) findViewById(R.id.export_seekbar);
        sbSeeker.setMax(1000);
		sbSeeker.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
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
            mTimerCount++;
            //App.showShortToast("timerTack: " + mTimerCount);
            if (1 == mTimerCount) firstTack();
            mTimerHandler.postDelayed(this, 999);
        }
    };



    private void firstTack() {
    }




	private void launchDefilming() {
		App.log("launchDefilming_11");
		mDesavi = new Desavi(new DesaviEventer());
		mDesavi.begin(App.gPlayPath, App.gPlayPass);
	}




	private void fulfilDefilming() {
		App.log("fulfilDefilming_11");

        int p = App.gPlayPath.indexOf(".");
        if (p < 1) return;
        App.gExportPath = App.gPlayPath.substring(0, p) + ".mp4";
        String name = Tools.getFileNameExt(App.gExportPath);
		tvName.setText(App.resstr(R.string.exporting) + ": " + name);

		// start the sample pulling
		int method = mDesavi.getMethod();
		DefilmA.create(method, mDesavi);
		if (null == DefilmA.get()) {
			if (null != mDesavi) {
				mDesavi.cease();
				mDesavi = null;
			}
			App.finishActivityAlert(this, "Error", "Unknown compression format");
			return;
		}

		mDemuxEventer = new DemuxEventer();
		DefilmA.get().setExportEventer(mDemuxEventer);
		DefilmA.get().attachScreen(mSurfview.getHolder());
		DefilmA.get().changeScreen(mSurfWidth, mSurfHeight);
		DefilmA.get().begin();
	}



	private void spongeDefilming() {
		App.gPlayPass = "";
		App.gRecordPass = "";
		finish();
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
						sLastResult = aResult;
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
						if (null == mDesavi) return;  // left over
						if (null == mBufDlg)
							showBufDlg(ExportActivity.this, "   Buffering ...  ");
						else {
							String text = "   Buffering ...  " + mDesavi.getHungryPercent() + "%";
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
				finish();
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



















	private void doCease() {
		if (null == DefilmA.get()) return;
        App.log( "stopping movie");
        DefilmA.get().cease();
        updateControls();
    }




    // Updates the on-screen controls to reflect the current state of the app.
    private void updateControls() {
        if (DefilmA.get() == null) return;

        if (!DefilmA.get().getLastFault().isEmpty()) {
            tvError.setText(DefilmA.get().getLastFault());
            tvError.setVisibility(View.VISIBLE);
        } else {
            tvError.setVisibility(View.GONE);
        }
    }



    private void updateProgress() {
        if (null == DefilmA.get()) return;

        long posi = DefilmA.get().getPosiSecs();
        tvPosi.setText(Tools.formatDuration(posi));

        int dura = DefilmA.get().getDuramils();
        tvDura.setText(Tools.formatDuration(dura));
        long prog = 0;
        if (dura > 0) {
            prog = 1000 * posi / dura;
            sbSeeker.setProgress((int) prog);
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
				sbSeeker.setProgress(1000);
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

   }







	private class DemuxEventer implements Defilm_1.ExportEvents {

		@Override
		public void onBegan() {
			//App.log("DemuxEventer.onBegan");
			mEnmuxer = new Enmuxer();
			mEnmuxer.attach(App.gExportPath + ".temp");
		}


		@Override
		public void onEnded(final boolean aCompleted) {
			//App.log("DemuxEventer.onEnded");
			mEnmuxer.detach();
			mEnmuxer = null;

			File oldFile = new File(App.gExportPath + ".temp");
			File newFile = new File(App.gExportPath);
			if (aCompleted) {
				sLastResult = 0;
				oldFile.renameTo(newFile);
			} else {
				oldFile.delete();
				if (DefilmA.get().isFaulted())
					sLastResult = -1;
				else
					sLastResult = 1;
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					finish();
					DefilmA.delete();
				}
			});
		}


		@Override
		public void onVideoFormat(MediaFormat aFormat) {
			//App.log("DemuxEventer.onVideoFormat");
			mEnmuxer.addVideoTrack(aFormat);
		}


		@Override
		public void onAudioFormat(MediaFormat aFormat) {
			//App.log("DemuxEventer.onAudioFormat");
			mEnmuxer.addAudioTrack(aFormat);
		}


		@Override
		public void onVideoSample(ByteBuffer aData, int aSize, long aMics) {
			//App.log("DemuxEventer.onVideoSample");
			if (!mEnmuxer.writeVideoSample(aData, aSize, aMics))
				DefilmA.get().setLastFault("Unknown video format.");
		}


		@Override
		public void onAudioSample(ByteBuffer aData, int aSize, long aMics) {
			//App.log("DemuxEventer.onAudioSample");
			mEnmuxer.writeAudioSample(aData, aSize, aMics);
		}
	}





	// * SurfaceHolder.Callback

	private class SurfaceEventer implements SurfaceHolder.Callback {
		@Override
		public void surfaceCreated(SurfaceHolder aHolder) {
			App.log( "surfaceCreated");
//			mSurfHolder = aHolder;
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
//			mSurfHolder = null;
			mSurfWidth = 0;
			mSurfHeight = 0;
			if (null != DefilmA.get())
				DefilmA.get().detachScreen();
		}

	}



}