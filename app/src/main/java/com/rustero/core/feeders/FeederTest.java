package com.rustero.core.feeders;


import android.util.Log;

import java.nio.ByteBuffer;


public class FeederTest {

	private final int READ_ROOM = 1000000;
	private Feeder myFeeder;
	private byte[] myBuffer = new byte[READ_ROOM];
	//private int mToseek = 2222222;
	private int mToseek = 0;


	private static void dlog(String aLine) {
		Log.d("FeederTest", aLine);
	}



	public void doTest(final String aPath) {
		new Thread() {
			public void run() {
				int result = test1(aPath);
				if (result == 0)
					dlog("selfTest ok");
				else
					dlog(" *** selfTest ERROR!");
			}
		}.start();
	}



	private static void sleep1() {
		try {
			Thread.sleep(1);
		} catch (Exception ex) {
			dlog("ex: " + ex.getMessage());
		}
	}



	private int test1(String aPath) {
		dlog("  11");
		int result = 0;

		if (aPath.contains("://"))
			myFeeder = new HttpFeeder();
		else
			myFeeder = new FileFeeder();
		myFeeder.begin(READ_ROOM, aPath);

		//first, create and get the size
		myFeeder.locus(0);
		myFeeder.request(0);
		while (myFeeder.busy())
			sleep1();
		result = myFeeder.getResult();
		if (result < 0) {
			dlog("error opening " + aPath);
			return result;
		}
		dlog("file size " + myFeeder.total());

		// second, read the head
		dlog("    requesting head " + 9999);
		myFeeder.locus(0);
		myFeeder.request(9999);
		result = drain();
		if (result < 0) {
			dlog("error getting head: " + result);
			return result;
		} else {
			dlog("head gotten: " + myFeeder.sofar() + ",  locus: " + myFeeder.locus());
		}

		// third, read the tail
		dlog("    requesting tail " + 11111);
		myFeeder.locus(myFeeder.total() - 11111);
		myFeeder.request(11111);
		result = drain();
		if (result < 0) {
			dlog("error getting tail: " + result);
			return result;
		} else {
			dlog("tail gotten: " + myFeeder.sofar() + ",  locus: " + myFeeder.locus());
		}

		// fourth, read the body
		dlog("    requesting body " + myFeeder.total());
		myFeeder.locus(0);
		myFeeder.request(myFeeder.total());
		result = drain();
		if (result < 0) {
			dlog("error getting body: " + result);
			return result;
		} else {
			dlog("body gotten: " + myFeeder.sofar() + ",  locus: " + myFeeder.locus());
		}

		// check the last few bytes
		if (myFeeder.sofar() == 10066709 && result > 4) {
			byte b1 = (byte) myBuffer[result - 4];
			if (myBuffer[result - 4] != (byte) 0x88)
				result = -999;
		}

		result = myFeeder.cease();
		dlog("  Closed " + aPath);
		return result;
	}




	private int drain() {
		int result = 0;

		while (myFeeder.busy()) {
			if (!myFeeder.hasResult()) {
				sleep1();
			} else {

				if ( mToseek > 0  &&  myFeeder.sofar() > mToseek) {
					mToseek = 0;
					myFeeder.cancel();
					myFeeder.locus(5000000);
					myFeeder.request(myFeeder.total());
					continue;
				}

				result = myFeeder.getResult();
				if (result < 0) {
					dlog("error reading at " + myFeeder.locus());
					return result;
				} else if (result > 0) {
					// get bufData
					try {
						ByteBuffer bybu = myFeeder.getBuffer();
						bybu.flip();
						bybu.get(myBuffer, 0, result);
						myFeeder.eaten();
					} catch (Exception ex) {
						dlog("ex: " + ex.getMessage());
					}
					dlog("count: " + result + ",  sofar: " + myFeeder.sofar() + ",  locus: " + myFeeder.locus());

				} else {
					dlog("waiting for outcome " + myFeeder.locus());
				}

			}
		}

		if (myFeeder.completed()) {
			dlog("reached the end");
		}
		return result;
	}


}



