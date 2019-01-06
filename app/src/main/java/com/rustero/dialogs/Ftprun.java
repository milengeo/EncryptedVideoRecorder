package com.rustero.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.rustero.App;
import com.rustero.R;
import com.rustero.gadgets.Tools;
import com.rustero.gadgets.FilmItem;
import com.rustero.gadgets.FtpServer;
import com.rustero.widgets.MessageBox;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamAdapter;

import java.io.File;
import java.io.FileInputStream;


public class Ftprun extends DialogFragment {


	public interface Eventer {
		void onDone(boolean aSuccess);
	}


	private static boolean sDialogShown = false;
	private static Ftprun self;
	private static Dialog sDialog;
	private static Worker sWorker;
	private static FilmItem sFiit;
	private static FtpServer sServer;

	private TextView tvFile, tvSofar, tvTotal;
	private SeekBar sbSeeker;
	private boolean mCancelled;



	public static boolean isActive() {
		return (null != sWorker);
	}



	public static void begin(FilmItem aFiit, FtpServer aFtpItem) {
		if (sDialogShown) return;
		AppCompatActivity activity = App.getActivity();
		if (null == activity) return;
		sFiit = aFiit;
		sServer = aFtpItem;

		self = new Ftprun();
		self.show(activity.getSupportFragmentManager(), "Ftprun");
		sWorker = self.new Worker();
		sWorker.start();
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		sDialogShown = true;
		self = this;
		View layout = inflater.inflate(R.layout.ftprun, container, false);
		setCancelable(false);
		sDialog = getDialog();
		sDialog.setCanceledOnTouchOutside(false);

		tvFile = (TextView) layout.findViewById(R.id.ftprun_file);
		tvFile.setText(sFiit.name);

		tvSofar = (TextView) layout.findViewById(R.id.ftprun_sofar);
		tvSofar.setText("0");

		tvTotal = (TextView) layout.findViewById(R.id.ftprun_total);
		tvTotal.setText(sFiit.bytes);

		sbSeeker = (SeekBar) layout.findViewById(R.id.ftprun_bar);
		sbSeeker.setMax(1000);
		sbSeeker.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});


		final Button cancelButton = (Button)layout.findViewById(R.id.ftprun_cancel);
		cancelButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (null == sWorker) return;
						sWorker.cancel();
					}
				}
		);

		return layout;
	}



	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		sDialogShown = false;
	}





	private class Worker extends Thread {

		private FTPClient mFtp;
		private boolean mDone;


		private void cancel() {
			if (null == mFtp) return;
			mCancelled = true;
			try {
				mFtp.abort();
			} catch (Exception e)	{
				e.printStackTrace();
			}
		}


		@Override
		public void run() {
			try {
				doUpload();
			} catch (Exception ex) {
			}
		}


		private void doUpload() {
			App.log("Ftprun-doUpload_11");
			if (null == sFiit) return;

			boolean success = false;
			try {
				mFtp = new FTPClient();
				mFtp.setBufferSize(0x10000);
				mFtp.setCopyStreamListener(streamListener);
				mFtp.connect(sServer.host);
				if (mFtp.login(sServer.user, sServer.pass)) {
					mFtp.enterLocalPassiveMode(); // important!
					mFtp.setFileType(FTP.BINARY_FILE_TYPE);
					mFtp.changeWorkingDirectory(sServer.dir);
					FileInputStream in = new FileInputStream(new File(sFiit.path));
					success = mFtp.storeFile(sFiit.name+".partial", in);
					if (success) {
						mFtp.deleteFile(sFiit.name);
						mFtp.rename(sFiit.name + ".partial", sFiit.name);
					}
					in.close();
					mFtp.logout();
					mFtp.disconnect();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (null != self)
				self.onDone(success);
			sWorker = null;
			App.log("Ftprun-doUpload_99");
		}
	}



	CopyStreamAdapter streamListener = new CopyStreamAdapter() {
		long mTick;

		@Override
		public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
			if (System.currentTimeMillis() - mTick < 500) return;
			mTick = System.currentTimeMillis();
			onProgress(totalBytesTransferred);
		}
	};



	private void onProgress(final long aSofar) {
			Activity activity = App.getActivity();
			if (null == activity) return;
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (null == self) return;
					self.tvSofar.setText(Tools.formatSize(aSofar));
					long prog = 0;
					if (aSofar > 0) {
						prog = 1000 * aSofar / sFiit.size;
						self.sbSeeker.setProgress((int) prog);
					}
				}
		});
	}



	public void onDone(final boolean aSuccess) {
		App.log("FtprunDialogEventer_onDone: " + aSuccess);
		if (null != sDialog) {
			sDialog.dismiss();
			sDialog = null;
		}
		Activity activity = App.getActivity();
		if (null == activity) return;
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
 				if (aSuccess) {
					MessageBox.show("Success", "Upload was successul!");
				} else {
					if (!mCancelled)
						MessageBox.show("Error!", "Error uploading file!");
				}
			}
		});
	}


}
