package com.rustero.core.feeders;


import android.util.Log;

import java.nio.ByteBuffer;


public abstract class Feeder extends Thread {


	protected int mRoom = 123456;
	protected volatile boolean mQuit2, mDone, mCancel;
	protected volatile int mRequest, mOutcome;

	protected String mPath;
	protected ByteBuffer mBuffer;
	protected long mTotal, mLocus;
	protected long mAsked, mSofar;



	protected void dlog(String aLine) { Log.d("Feeder", aLine); }



	// * wanters



	public void cancel() {
		mCancel = true;
		hold();
	}



	public void hold() {
		while (0 != mRequest) {
			if (mQuit2) return;
			try {
				Thread.sleep(1);
			} catch (Exception ex) {
			}
		}
	}



	public boolean done() {
		return mDone;
	}



	public boolean busy() {
		if (0 == mRequest) return false;
		return true;
	}



	public boolean hasResult() {
		if (0 == mOutcome) return false;
		return true;
	}



	public int getResult() {
		return mOutcome;
	}



	public ByteBuffer getBuffer() {
		return mBuffer;
	}



	public int begin(int aRoom, String aPath) {
		mRoom = aRoom;
		mPath = aPath;
		mBuffer = ByteBuffer.allocateDirect(mRoom);
		start();      //the thread
		return 0;
	}



	public int cease() {
		mQuit2 = true;
		return 0;
	}



	public int request(long aAsked) {
		mRequest = 0;
		mOutcome = 0;

		mSofar = 0;
		mAsked = aAsked;
		if (mTotal > 0) {
			if (mLocus + mAsked > mTotal)
				mAsked = mTotal - mLocus;
		}

		mRequest = 1;
		return 0;
	}



	public void eaten() {
		mOutcome = 0;
	}



	public boolean completed() {
		if (mAsked > 0  &&  mSofar >= mAsked) return true;
		return false;
	}



	public long sofar() {
		return mSofar;
	}

	

	public long total() {
		return mTotal;
	}



	public long locus() {
		return mLocus;
	}



	public void locus(long aLocus) {
		mLocus = aLocus;
	}





	// * doers



	@Override
	public void run() {
		try {
			while (!mQuit2) {
				Thread.sleep(1);
				if (mRequest == 0) continue;  // there is no request

				mCancel = false;
				perform();
			}
		} catch (Exception ex) {
			dlog("ex: " + ex.getMessage());
		}

		detach();
		mDone = true;
	}



	abstract public  boolean isSlow();


	abstract protected void detach();


	abstract protected void perform();


}
