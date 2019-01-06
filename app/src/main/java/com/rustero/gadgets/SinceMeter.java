package com.rustero.gadgets;



public class SinceMeter {
	private long mKick;


	public void reset() {
		mKick = 0;
	}


	public long sinceMils() {
		if (0 == mKick) {
			mKick = System.currentTimeMillis();
		}
		long result = System.currentTimeMillis() - mKick;
		return result;
	}


	public void click() {
		mKick = System.currentTimeMillis();
	}

}
