package com.rustero.savi2api;


import com.rustero.App;
import com.rustero.gadgets.Tools;

public class SaviSampler {

	private long mKickMils=-1, mLasttamp=-1;
	public int videoCount, audioCount, syncoCount;
	public long videoBytes, audioBytes;
	public long takeKick;


	SaviSampler()
	{
		takeKick = System.currentTimeMillis();
	}


	void setLastStamp(long aStamp) {
		mLasttamp = aStamp;
		if (mKickMils < 0)
			mKickMils = aStamp;
	}




	long countedDuration() {
		long result = mLasttamp - mKickMils - 1;
		return result;
	}



	void report() {
		long took = System.currentTimeMillis() - takeKick;
		App.log(String.format(" = took: %d mils", took));
		App.log("duration: " + Tools.formatDuration(countedDuration()/1000));
		App.log("video count: " + videoCount);
		App.log("sync frames: " + syncoCount);
		App.log("video bytes: " + Tools.formatSize(videoBytes));

		if (audioCount < 1) return;
		App.log("audio count: " + audioCount);
		App.log("audio bytes: " + Tools.formatSize(audioBytes));
	}
}




