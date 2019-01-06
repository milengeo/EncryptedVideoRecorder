package com.rustero.savi2api;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.rustero.App;
import com.rustero.Errors;
import com.rustero.core.feeders.Feeder;
import com.rustero.core.feeders.FileFeeder;
import com.rustero.core.feeders.HttpFeeder;
import com.rustero.gadgets.Lmx;
import com.rustero.gadgets.Tools;

import java.nio.ByteBuffer;


public class Desavi {


	public interface Eventer {
		void onCreate(int aResult);
		void onBuffer(boolean aOn);
	}

	private Eventer mEventer;
	private volatile int mResult = 0;
	private volatile boolean mQuit;
	private int mVideoTrack=-1, mAudioTrack=-1;
	private int mTrackCount;
	private MediaFormat mVideoFormat, mAudioFormat;
	private Feeder mFeeder;
	private long mTotalSize;
	private String mPath, mPass;
	private boolean mHungry;
	private int mFeedRoom, mHungryPercent;


	private static void dlog(String aLine) { Log.d("Desavi", aLine); }



    public Desavi(Eventer aEventer) {
		mEventer = aEventer;
    }



	public void begin(String aPath, String aPass) {
		mPath = aPath;
		mPass = aPass;
		if (mPath.contains("://")) {
			mFeeder = new HttpFeeder();
		} else {
			mFeeder = new FileFeeder();
		}

		new Thread() {
			public void run() {
				doCreate();
				mEventer.onCreate(mResult);
			}
		}.start();
	}



	private void doCreate() {
		if (mPass.length() < 6) {
			mResult = Errors.WRONG_PASSWORD;
			return;
		}

		App.gSavi2Lib.createSaviPicker();
		mFeedRoom = App.gSavi2Lib.getSaviFeedRoom();
		mFeeder.begin(mFeedRoom, mPath);

		//load the head
		App.gSavi2Lib.resetSavi();
		mFeeder.locus(0);
		mFeeder.request(9999);
		isHungry();
		haulFeed();
		if (mResult < 0) return;

		mResult = App.gSavi2Lib.createSaviDetracker(mPass);
		if (mResult < 0) return;
		if (mQuit) return;

		dlog("method: " + getMethod());

		//load the roster
		long rosterLocus = getRosterLocus();
		App.gSavi2Lib.resetSavi();
		int count = App.gSavi2Lib.getSaviPickerCount();

		mFeeder.locus(rosterLocus);
		mFeeder.request(0);
		haulFeed();
		isHungry();

		count = App.gSavi2Lib.getSaviPickerCount();

		int seeks = loadRoster();
		if (seeks < 1) {
			mResult = Errors.UNKNOWN_FILE_FORMAT;
			return;
		}
		mTotalSize = mFeeder.total();
	}



	public void cease() {
		mQuit = true;
		try {
			App.gSavi2Lib.deleteSaviDetracker();
		} catch (Exception ex) {
			dlog("ex: " + ex.getMessage());
		}
		mFeeder.cease();
	}



	private void haulFeed() {
		while (mFeeder.busy()) {
			if (mFeeder.done()) break;
			try { Thread.sleep(1); } catch (Exception ex) {}
			pumpFeed();
		}
		mResult = mFeeder.getResult();
	}



	public int pumpFeed() {
		int result = 0;

		while (mFeeder.hasResult()) {
			result = mFeeder.getResult();
			if (result < 0) {
				dlog("error reading at " + mFeeder.locus());
				return result;
			} else if (result > 0) {
				// get bufData
				try {
					int spare = App.gSavi2Lib.getSaviPickerSpare();
					if (result < 0) return result;
					if (result < spare) {
						//dlog("pumpFeed, locus: "+mFeeder.locus() + ",  result: "+result + ",  spare:"+spare);
						ByteBuffer bybu = mFeeder.getBuffer();
						int offset = bybu.arrayOffset();
						int remain = bybu.remaining();
						bybu.flip();
						App.gSavi2Lib.feedSaviPicker(bybu, result);
						mFeeder.eaten();
					} else {
						//dlog("small spare, locus: "+mFeeder.locus() + ",  result: "+result + ",  spare:"+spare);
						return 0;
					}
				} catch (Exception ex) {}
			}
		}

		return result;
	}



	public long getTotalSize() {
		return mTotalSize;
	}



	public int getHungryPercent() {
		return mHungryPercent;
	}



	public boolean isHungry() {
		int count = App.gSavi2Lib.getSaviPickerCount();
		if (mHungry) {
			if (mFeeder.done() || mFeeder.completed()  ||  count > mFeedRoom) {
				mHungry = false;
				if (mFeeder.isSlow())
					mEventer.onBuffer(false);
				dlog("  mHungry = false, " + count);
			}
		} else {
			if (!mFeeder.completed()  &&  count < mFeedRoom/2) {
				mHungry = true;
				dlog("  mHungry = true, " + count);
			}
		}

		if (mHungry  &&  mFeeder.isSlow()) {
			mHungryPercent = (int) (100.0f * count / mFeedRoom);
			//dlog("  mHungryPercent: " + mHungryPercent + ",  count: " + count + ", mFeedRoom:  " + mHungryRoom);
			mEventer.onBuffer(true);
		}

		return mHungry;
	}



	public int getMethod() {
		return App.gSavi2Lib.getSaviMethod();
	}



	public int getDuration() {
		int result = App.gSavi2Lib.getSaviDuration();
		return result;
	}



	public MediaFormat getVideoFormat() {
		mVideoFormat = new MediaFormat();

		String pars = App.gSavi2Lib.getVideoFormat();
		if (pars.isEmpty()) {
			dlog("Errors - unknown video format!");
			return null;
		}
		Lmx lmx = new Lmx(pars);
		String mime = lmx.getStr("mime");
		mVideoFormat.setString("mime", mime);

		mVideoTrack = lmx.getInt("track");
		int width = lmx.getInt("width");
		mVideoFormat.setInteger("width", width);

		int height = lmx.getInt("height");
		mVideoFormat.setInteger("height", height);

		int turn90 = lmx.getInt("turn90");
		if (0 != turn90)
			mVideoFormat.setInteger("turn90", 90);

		String topic = lmx.getStr("topic");
		mVideoFormat.setString("topic", topic);

		ByteBuffer dbb = ByteBuffer.allocateDirect(9999);
		int csd_size = 0;
		dbb.clear();
		csd_size = App.gSavi2Lib.getVideoCsd0(dbb);
		if (csd_size > 0) {
			dbb.position(csd_size);
			dbb.flip();
			ByteBuffer csd_0 = Tools.copyDirect(dbb);
			mVideoFormat.setByteBuffer("csd-0", csd_0);
		}

		dbb.clear();
		csd_size = App.gSavi2Lib.getVideoCsd1(dbb);
		if (csd_size > 0) {
			dbb.position(csd_size);
			dbb.flip();
			ByteBuffer csd_1 = Tools.copyDirect(dbb);
			mVideoFormat.setByteBuffer("csd-1", csd_1);
		}

		return mVideoFormat;
	}




	public MediaFormat getAudioFormat() {
		mAudioFormat = new MediaFormat();

		String code = App.gSavi2Lib.getAudioFormat();
		if (code.isEmpty()) {
			dlog("Errors - unknown audio format!");
			return null;
		}
		Lmx lmx = new Lmx(code);
		String mime = lmx.getStr("mime");
		mAudioFormat.setString("mime", mime);

		mAudioTrack = lmx.getInt("track");
		int channelCount = lmx.getInt("channel-count");
		mAudioFormat.setInteger("channel-count", channelCount);

		int sampleRate = lmx.getInt("sample-rate");
		mAudioFormat.setInteger("sample-rate", sampleRate);

		ByteBuffer dbb = ByteBuffer.allocateDirect(9999);
		int csd_size = 0;
		dbb.clear();
		csd_size = App.gSavi2Lib.getAudioCsd0(dbb);
		if (csd_size > 0) {
			dbb.position(csd_size);
			dbb.flip();
			ByteBuffer csd_0 = Tools.copyDirect(dbb);
			mAudioFormat.setByteBuffer("csd-0", csd_0);
		}

		return mAudioFormat;
	}



//	public int getTrackCount() {
//		mTrackCount = 0;
//		if (mVideoTrack > 0)
//			mTrackCount++;
//		if (mAudioTrack > 0)
//			mTrackCount++;
//		return mTrackCount;
//	}



	public int getVideoTrack() {
		return mVideoTrack;
	}



	public int getAudioTrack() {
		return mAudioTrack;
	}




	public boolean ended() {
		return App.gSavi2Lib.endedSavi();
	}



	public int advance() {
		return App.gSavi2Lib.advanceSavi();
	}



	public int seekto(int aStamp) {
		mFeeder.cancel();
		int result = App.gSavi2Lib.resetSavi();
		long locus = getSeekLocus(aStamp);
		mFeeder.locus(locus);
		mFeeder.request(mTotalSize);
		isHungry();
		return result;
	}



	public long getSeekLocus(int aStamp) {
		return App.gSavi2Lib.getSaviSeekLocus(aStamp);
	}



	public long getRosterLocus() {
		return App.gSavi2Lib.getSaviRosterLocus();
	}



	public int loadRoster() {
		int result = App.gSavi2Lib.loadSaviRoster();
		return result;
	}



	public int getSampleTrack() {
		return App.gSavi2Lib.getSaviSampleTrack();
	}



	public long getSampleTime() {
		return App.gSavi2Lib.getSaviSampleTime();
	}



	public int getSampleSize() {
		return App.gSavi2Lib.getSaviSampleSize();
	}



	public int getSampleFlags() {
		int result = 0;
		if (App.gSavi2Lib.isSyncSample())
			result |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
		return result;
	}



	public int readSampleData(ByteBuffer aBuffer) {
		return App.gSavi2Lib.readSaviSampleData(aBuffer);
	}









	public static class SelfTest {

		static private String sPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/opit1.dat";
		//static private String sPath = "http://rustero.com/opiti/opit1.dat";

		static private MediaFormat sVideoFormat, sAudioFormat;
		static private int sVideoTrack=-1, sAudioTrack=-1;
		static private VideoStuff videoStuff = new VideoStuff();
		static private AudioStuff audioStuff = new AudioStuff();

		static private Desavi sDesavi;
		static private int sSampleSize;
		static private ByteBuffer sPail = ByteBuffer.allocateDirect(333333);
		static private Activity sActivity;
		static private SaviSampler sSampler;
		static private ProgressDialog sWaitDlg = null;
		static private int sToseek = 5000;




		public static boolean doTest1(Activity aActivity) {
			sActivity = aActivity;
			dlog("    doTest1_11");
			dlog("Desavi.SelfTestApi.doTest1");
			int result = 0;

			sDesavi = new Desavi(new TestEventer());
			sDesavi.begin(sPath, "opit12");

			dlog("    doTest1_99");
			return true;
		}




		public static class TestEventer implements Eventer {

			public void onCreate(final int aResult) {
				sActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (0 == aResult) {
							dlog("onCreate success.");

							new Thread() {
								public void run() {
									doTest2();
								}
							}.start();

						} else {
							dlog(" *** onCreate error: " + Errors.getText(aResult));
							Toast.makeText (sActivity, " *** " + Errors.getText(aResult), Toast.LENGTH_SHORT).show();
						}

					}
				});
			}


			public void onBuffer(final boolean aOn) {
				sActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (aOn) {
							//Toast.makeText (sActivity, "ON", Toast.LENGTH_SHORT).show();
							if (null == sWaitDlg)
								showWaitDlg(sActivity, "   Buffering ...  ");
							else {
								String text = "   Buffering ...  " + sDesavi.getHungryPercent() + "%";
								sWaitDlg.setMessage(text);
							}
						} else {
							//Toast.makeText (sActivity, "off", Toast.LENGTH_SHORT).show();
							hideWaitDlg();
						}
					}
				});
			}


		}



		private static void doTest2() {
			dlog("    doTest2_11");
			sSampler = new SaviSampler();
			long lastSay = System.currentTimeMillis();

			if (!takeFormat()) {
				dlog("Errors taking format");
				return;
			}

			// start the sample pulling
			sDesavi.seekto(0);

			while (true) {
				try { Thread.sleep(1); } catch (Exception ex) {}

				if (sDesavi.pumpFeed() < 0) return;
				if (sDesavi.isHungry()) {
					continue;
				}

				long sampleTime = sDesavi.getSampleTime();
				if (sampleTime < 0) {
					break;
				}

				sSampleSize = sDesavi.getSampleSize();
				int track = sDesavi.getSampleTrack();

				sSampler.setLastStamp(sampleTime);
				if (track == sVideoTrack) {
					int flags = sDesavi.getSampleFlags();
					sSampler.videoCount++;
					sSampler.videoBytes += sSampleSize;
					if (0 != (flags & MediaCodec.BUFFER_FLAG_KEY_FRAME))
						sSampler.syncoCount++;

				} else if (track == sAudioTrack) {
					sSampler.audioCount++;
					sSampler.audioBytes += sSampleSize;
				}
				verifySample();

				if (sDesavi.advance() < 0) {
					dlog("cannot advance");
					break;
				}

				if (System.currentTimeMillis() - lastSay > 999) {
					lastSay = System.currentTimeMillis();
					int count = App.gSavi2Lib.getSaviPickerCount();
					dlog("sofar: " + Tools.formatDuration(sSampler.countedDuration()/1000) + "  " + Tools.formatSize(sSampler.videoBytes) + "  b: " + count);
				}
			}

			sDesavi.cease();

			sSampler.report();
			dlog("    doTest2_99");
		}



		static public void showWaitDlg(Context aContext, String aMessage) {
			sWaitDlg = new ProgressDialog(aContext);
			sWaitDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			sWaitDlg.setMessage(aMessage);
			sWaitDlg.setCanceledOnTouchOutside(false);
			sWaitDlg.setCancelable(false);
			sWaitDlg.show();
			Window window = sWaitDlg.getWindow();
			window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
		}


		static public void hideWaitDlg() {
			if (null == sWaitDlg) return;
			sWaitDlg.dismiss();
			sWaitDlg = null;
		}



		static boolean takeFormat() {
			int duration = sDesavi.getDuration();
			dlog("duration: " + Tools.formatDuration(duration/1000));

			// video format
			sVideoFormat = sDesavi.getVideoFormat();
			if (null == sVideoFormat) {
				dlog("Unknown video format!");
				return false;
			}

			sVideoTrack = sDesavi.getVideoTrack();
			if (sVideoTrack != 1)
				throw new IllegalStateException("Wrong video track.");

			videoStuff.mime = sVideoFormat.getString("mime");
			if (!videoStuff.mime.equals("video/avc")) return false;

			videoStuff.width = sVideoFormat.getInteger("width");
			if (videoStuff.width != 777) return false;

			videoStuff.height = sVideoFormat.getInteger("height");
			if (videoStuff.height != 555) return false;

			videoStuff.topic = sVideoFormat.getString("topic");
			dlog("topic: " + videoStuff.topic);

			ByteBuffer videoCsd_0 = sVideoFormat.getByteBuffer("csd-0");
			verifyBuffer(videoCsd_0, 0);
			ByteBuffer videoCsd_1 = sVideoFormat.getByteBuffer("csd-1");
			verifyBuffer(videoCsd_1, 0);


			// audio format
			sAudioFormat = sDesavi.getAudioFormat();
			if (null == sAudioFormat) {
				dlog("Unknown audio format!");
				return false;
			}

			sAudioTrack = sDesavi.getAudioTrack();
			if (sAudioTrack != 2)
				throw new IllegalStateException("Wrong audio track.");

			audioStuff.mime = sAudioFormat.getString("mime");
			if (!audioStuff.mime.equals("audio/mp4a-latm")) return false;

			audioStuff.channelCount = sAudioFormat.getInteger("channel-count");
			if (audioStuff.channelCount != 1) return false;

			audioStuff.sampleRate = sAudioFormat.getInteger("sample-rate");
			if (audioStuff.sampleRate != 44100) return false;

			ByteBuffer audioCsd_0 = sAudioFormat.getByteBuffer("csd-0");
			verifyBuffer(audioCsd_0, 0);

			return true;
		}



		static class VideoStuff {
			String mime, topic;
			int width, height;
			byte[] csd0, csd1;
		}



		static class AudioStuff {
			String mime;
			int sampleRate, channelCount;
			byte[] csd0;
		}



		static private void verifySample() {
			if (sSampleSize > 0) {
				int dataSize = 0;
				if (sPail.capacity() < sSampleSize)
					sPail = ByteBuffer.allocateDirect(sSampleSize * 2);
				sPail.clear();
				dataSize = sDesavi.readSampleData(sPail);
				sPail.position(dataSize);
				sPail.flip();
				verifyBuffer(sPail, dataSize - 333);
			}
		}



		static private void verifyBuffer(ByteBuffer aBuffer, int aFrom) {
			byte value=0;
			aBuffer.position(0);
			for (int i=aFrom; i<aBuffer.limit(); i++) {
				value = aBuffer.get(i);
				if (value != (byte) i)
					throw new IllegalStateException("Wrong buffer value!");
			}
			aBuffer.position(0);
		}






	}




}
