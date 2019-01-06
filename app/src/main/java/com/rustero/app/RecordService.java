package com.rustero.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.Html;

import com.rustero.App;
import com.rustero.R;
import com.rustero.core.coders.EnfilmA;
import com.rustero.core.coders.Enfilm_1;
import com.rustero.gadgets.Tools;


public class RecordService extends Service {



	public static RecordActivity activity = null;


	public RecordService() {}



	@Override
	public void onCreate() {
		super.onCreate();
	}



	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (null == intent) return 0;
		if (intent.getAction().equals(App.INTENT_START_SERVICE)) {
			doStartService();
		} else if (intent.getAction().equals(App.INTENT_STOP_SERVICE)) {
			doStopService();
		} else if (intent.getAction().equals(App.INTENT_BEGIN_RECORDING)) {
			doBeginRecording();
		} else if (intent.getAction().equals(App.INTENT_CEASE_RECORDING)) {
			doCeaseRecording();
		} else if (intent.getAction().equals(App.INTENT_UPDATE_ORIENTATION)) {
	//		doUpdateOrientation();
		}
		return START_STICKY;
	}



	@Override
	public void onDestroy() {
		super.onDestroy();
		App.log("In onDestroy");
	}



	@Override
	public IBinder onBind(Intent intent) {
		// Used only in case of bound services.
		return null;
	}



	// * doers



	private void doStartService() {
		App.log("doStartService");
		Enfilm_1.get().attachEngine(ENFILM_EVENTER);
		startForeground(App.NOTIFICATION_BAR_RECORD, buildNotification("Recording..."));
//		Enfilm_1.get().updateRotation();
	}



	private void doStopService() {
		App.log("doStopService");
		Enfilm_1.get().detachEngine();
		stopForeground(true);
		stopSelf();
	}



	private void doBeginRecording() {
		Enfilm_1.get().begin();
		App.gRecordedPath = Enfilm_1.get().getOutputName();
	}


	private void doCeaseRecording() {
		Enfilm_1.get().cease();
	}





	private Notification buildNotification(String aText) {
		Intent notificationIntent = new Intent(this, RecordActivity.class);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.app_icon_96);

		String title = "" + Html.fromHtml("<b>" + App.resstr(R.string.app_name) + "</b>");
		Notification notification = new NotificationCompat.Builder(this)
				.setTicker(title)
				.setContentTitle(title)
				.setContentText(aText)
				.setSmallIcon(R.drawable.app_icon_96)
				.setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
				.setContentIntent(pendingIntent)
				.build();

		return notification;
	}




	private void updateNotification() {
		EnfilmA.StatusC status = Enfilm_1.get().getStatus();
		if (null == status) return;

		String text = Enfilm_1.get().getOutputName();
		text += "  " + String.format("%02d:%02d", status.secs/60, status.secs%60);
		text += "  " + Tools.formatSize(status.size);

		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(App.NOTIFICATION_BAR_RECORD, buildNotification(text));
	}






	private Enfilm_1.Events ENFILM_EVENTER = new Enfilm_1.Events() {


		public void onStateChanged() {
			if (null == activity) return;
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					activity.updateUI();
				}
			});
		}


		public void onProgress() {
			if (Enfilm_1.get().getStatus().secs > App.getRecordMinutes() * 60)
				Enfilm_1.get().cease();

			if (null == activity) {
				new Thread() {
					public void run() {
						updateNotification();
					}
				}.start();

			} else {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						activity.updateStatus();
					}
				});
			}
		}



		public void onFault(final String aMessage)
		{
			if (null == activity) return;
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					new android.app.AlertDialog.Builder(activity)
							.setTitle("Recording error")
							.setMessage(aMessage)
							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									// continue with click
									activity.finish();
								}
							})
							.setIcon(android.R.drawable.ic_dialog_alert)
							.show();
				}
			});
		}

	};



//
//	private class OrientationEventer extends OrientationEventListener {
//
//		public OrientationEventer() {
//			super(RecordService.this, SensorManager.SENSOR_DELAY_NORMAL);
//		}
//
//
//
//		public void onOrientationChanged(int orientation) {
//			if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;
//			int degrees = 270;
//			if (orientation < 45 || orientation >315) degrees=0;
//			else if (orientation < 135)
//				degrees = 90;
//			else if (orientation < 225)
//				degrees = 180;
//			if (mOrientationAngle == degrees) return;
//			mOrientationAngle = degrees;
//			//App.log("mOrientationAngle: " + mOrientationAngle);
//
////			Enfilm_1.get().updateRotation();
//
////			if (null != activity) {
////			activity.postDelayed(new Runnable() {
////				public void run() {
////					Enfilm_1.get().updateRotation();
////				}
////			}, 999);
////			}
//		}
//
//	};
//


}
