package com.rustero.core.feeders;


import android.util.Log;

import com.rustero.Errors;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;


public class FileFeeder  extends Feeder {

	protected FileInputStream mStream;
	private FileChannel mChannel;



	protected void dlog(String aLine) { Log.d("FileFeeder", aLine); }



	// * doers

	public boolean isSlow() {
		return false;
	}



	protected boolean attach() {
		try {
			try {
				mStream = new FileInputStream(mPath);
				mChannel = mStream.getChannel();
				mTotal = mStream.available();
			} catch (Exception ex) {
				mOutcome = Errors.OPEN_FILE;
			}
		} catch (Exception ex) {
			dlog(" * ex_FileFeeder_attach: " + ex.getMessage());
		}

		if (mOutcome < 0) {
			mRequest = 0;
			return false;
		} else
			return true;
	}



	protected void detach() {
		if (null == mStream) return;
		try {
			try {
				mStream.close();
			} catch (Exception ex) {}
		} catch (Exception ex) {
			dlog(" * ex_FileFeeder_detach: " + ex.getMessage());
		}
	}



	protected void perform() {
		try {
			if (!attach()) return;

			mChannel.position(mLocus);
			while (!mQuit2) {

				if (mCancel) {
					break;
				}

				if (mOutcome != 0) {
					Thread.sleep(1);  // outcome is not used
					continue;
				}

				if (completed()) {
					break;
				}

				mBuffer.clear();
				if (mAsked > 0) {
					long left = mAsked - mSofar;
					if (left < mRoom)
						mBuffer.limit((int) left);
				}

				int count = mChannel.read(mBuffer);
				if (count < 0) {
					if (0 == mTotal)
						mTotal = mLocus;
					break;  // end of file
				} else if (count > 0) {
					mSofar += count;
					mLocus += count;
				}
				mOutcome = count;
			}
		} catch (Exception ex) {
			dlog("ex: " + ex.getMessage());
			mOutcome = Errors.OPEN_FILE;
		}

		mRequest = 0;
	}

}
