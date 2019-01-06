package com.rustero.savi2api;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;

import com.rustero.App;
import com.rustero.Errors;
import com.rustero.gadgets.Lmx;
import com.rustero.gadgets.Tools;

import java.nio.ByteBuffer;


public class Ensavi {

	private String mPath;
	private int mVideoTrack=-1, mAudioTrack=-1;
	private long mVideoKick, mVideoMils, mAudioKick, mAudioMils;


	public Ensavi() {
	}


	public int createWriter(String aPath) {
		mPath = aPath;
		int result = App.gSavi2Lib.createSaviWriter(mPath);
		if (result < 0) {
			App.log("Error creating output file!");
			return result;
		}
		return result;
	}



	public int createEntracker(String aPassword) {
		if (aPassword.length() < 6) return -1;
		int result = App.gSavi2Lib.createSaviEntracker(aPassword);
		if (result < 0) {
			App.log("Error creating output format!");
			return result;
		}
		return result;
	}



	public void deleteEntracker() {
		App.gSavi2Lib.deleteSaviEntracker();
		App.log(" * Ensavi_deleteEntracker");
	}



//	public String getErrorText() {
//		return App.gSavi2Lib.getSaviErrorText();
//	}



	public int getMethod() {
		return App.gSavi2Lib.getSaviMethod();
	}



	public int addVideoFormat(MediaFormat aFormat) {
		if (null == aFormat) return -1;
		try {
			Lmx lmx = new Lmx();

			String mime = aFormat.getString("mime");
			lmx.addStr("mime", mime);

			int width = aFormat.getInteger("width");
			lmx.addInt("width", width);

			int height = aFormat.getInteger("height");
			lmx.addInt("height", height);

			if (aFormat.containsKey("turn90")) {
				int turn90 = aFormat.getInteger("turn90");
				lmx.addInt("turn90", turn90);
			}

			String topic = aFormat.getString("topic");
			if (null != topic)
				lmx.addStr("topic", topic);

			String code = lmx.getCode();
			mVideoTrack = App.gSavi2Lib.addVideoFormat(code);

			ByteBuffer bb0 = aFormat.getByteBuffer("csd-0");
			if (null != bb0) {
				ByteBuffer csd_0 = Tools.copyDirect(bb0);
				App.gSavi2Lib.setVideoCsd0(csd_0.limit(), csd_0);
			}

			ByteBuffer bb1 = aFormat.getByteBuffer("csd-1");
			if (null != bb1) {
				ByteBuffer csd_1 = Tools.copyDirect(bb1);
				App.gSavi2Lib.setVideoCsd1(csd_1.limit(), csd_1);
			}

			App.log(" * Ensavi_addVideoFormat");
			return mVideoTrack;
		} catch (Exception ex) {
			App.log( " *****_ex Ensavi_addVideoFormat: " + ex.getMessage());
			return -1;
		}
	}



	public int addAudioFormat(MediaFormat aFormat) {
		if (null == aFormat) return -1;
		Lmx lmx = new Lmx();

		String mime = aFormat.getString("mime");
		lmx.addStr("mime", mime);

		int channelCount = aFormat.getInteger("channel-count");
		lmx.addInt("channel-count", channelCount);

		int sampleRate = aFormat.getInteger("sample-rate");
		lmx.addInt("sample-rate", sampleRate);

		String code = lmx.getCode();
		mAudioTrack = App.gSavi2Lib.addAudioFormat(code);

		ByteBuffer bb0 = aFormat.getByteBuffer("csd-0");
		if (null != bb0) {
			ByteBuffer csd_0 = Tools.copyDirect(bb0);
			App.gSavi2Lib.setAudioCsd0(csd_0.limit(), csd_0);
		}

		App.log(" * Ensavi_addAudioFormat");
		return mAudioTrack;
	}



	public int start() {
		App.log(" * Ensavi_start");
		return App.gSavi2Lib.startSavi();
	}



	public int finish() {
		App.log(" * Ensavi_finish");
		return App.gSavi2Lib.finishSavi();
	}




	public int writeVideoSample(ByteBuffer byteBuf,  MediaCodec.BufferInfo bufferInfo) {
		mVideoMils = bufferInfo.presentationTimeUs / 1000;
		if (mVideoKick == 0) {
			mVideoKick = mVideoMils;
			App.log(" *** Ensavi mVideoKick secs: " + mVideoKick /1000);
		}
		int sampleMils = (int) (mVideoMils - mVideoKick);
		//App.log("writeVideoSample: " + sampleMils + "v  " + mVideoMils + ":" + mVideoKick);

		boolean syncFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
		int result = App.gSavi2Lib.writeVideoSample(sampleMils, bufferInfo.size, byteBuf, syncFrame);

		if (syncFrame) {
			App.log(" * Ensavi_writeVideoSample sync frame: " + sampleMils);
		}

		return result;
	}



	public int writeAudioSample(ByteBuffer byteBuf,  MediaCodec.BufferInfo bufferInfo) {
		if (mVideoKick == 0) return 0;  // wait for video kickoff

		mAudioMils = bufferInfo.presentationTimeUs / 1000;
		if (mAudioKick == 0) {
			mAudioKick = mAudioMils;
			App.log(" *** Ensavi mAudioKick secs: " + mAudioKick /1000);
		}

		int sampleMils = (int) (mAudioMils - mAudioKick);
		//App.log("writeAudioSample: " + sampleMils + "a  " + mAudioMils + ":" + mAudioKick);

		int result = App.gSavi2Lib.writeAudioSample(sampleMils, bufferInfo.size, byteBuf);
		return result;
	}



	public int getDoneMils() {
		int result = (int) (mVideoMils - mVideoKick);
		return result;
	}






	static public class SelfTest {

		static private String mTestPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/opit1.dat";
		static private long recordMils = 0;
		static private SaviSampler sampler;

		static private ByteBuffer bufData;
		static MediaCodec.BufferInfo bufInfo = new MediaCodec.BufferInfo();
		static Ensavi ensaviTest;


		static public boolean doTest1() {
			App.log(" ");
			App.log("Ensavi.SelfTestApi.doTest1");
			//final int SECONDS = 66;
			final int SECONDS = 11;

			ByteBuffer videoCsd0 = ByteBuffer.allocateDirect(33);
			fillupBuffer(videoCsd0);
			ByteBuffer videoCsd1 = ByteBuffer.allocateDirect(22);
			fillupBuffer(videoCsd1);
			ByteBuffer audioCsd0 = ByteBuffer.allocateDirect(11);
			fillupBuffer(audioCsd0);

			final int TEST_SIZE = 500000;
			bufData = ByteBuffer.allocateDirect(TEST_SIZE);
			fillupBuffer(bufData);

			long lastSay = System.currentTimeMillis();
			sampler = new SaviSampler();
			sampler.setLastStamp(0);

			ensaviTest = new Ensavi();

			int result = ensaviTest.createWriter(mTestPath);
			if (result < 0) {
				App.log(Errors.getText(result));
				return false;
			}

			if (0 > ensaviTest.createEntracker("opit12")) {
				App.log("Error - uknown format!");
				return false;
			}
			App.log("method: " + ensaviTest.getMethod());

			// video format
			MediaFormat videoFormat = new MediaFormat();
			videoFormat.setString("mime", "video/avc");
			videoFormat.setInteger("width", 777);
			videoFormat.setInteger("height", 555);
			videoFormat.setString("topic", "Ala bala nica,\nturska panica.");
			videoFormat.setByteBuffer("csd-0", videoCsd0);
			videoFormat.setByteBuffer("csd-1", videoCsd1);

			int videoTrack = ensaviTest.addVideoFormat(videoFormat);
			if (videoTrack != 1)
				throw new IllegalStateException("Wrong video track!");


			// audio format
			MediaFormat audioFormat = new MediaFormat();
			audioFormat.setString("mime", "audio/mp4a-latm");
			audioFormat.setInteger("channel-count", 1);
			audioFormat.setInteger("sample-rate", 44100);
			audioFormat.setByteBuffer("csd-0", audioCsd0);

			int audioTrack = ensaviTest.addAudioFormat(audioFormat);
			if (audioTrack != 2)
				throw new IllegalStateException("Wrong audio track!");


			// samples
			ensaviTest.start();
			for (int i=0; i<SECONDS; i++) {
				write1Second();

				if (System.currentTimeMillis() - lastSay > 999) {
					lastSay = System.currentTimeMillis();
					App.log("sofar: " + Tools.formatDuration(sampler.countedDuration()/1000) + "  " + Tools.formatSize(sampler.videoBytes));
				}
			}

			ensaviTest.finish();
			ensaviTest.deleteEntracker();

			sampler.report();
			return true;
		}



		static void write1Second() {
			int intraSize = 234567;
			int interSize = 23456;
			int audioSize = 333;

			bufInfo.presentationTimeUs = recordMils * 1000;
			bufInfo.size = intraSize;
			bufInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
			ensaviTest.writeVideoSample(bufData, bufInfo);

			recordMils += 40;
			sampler.syncoCount++;
			sampler.videoCount++;
			sampler.videoBytes += bufInfo.size;

			for (int i = 0; i < 25; i++) {
				bufInfo.presentationTimeUs = recordMils * 1000;
				bufInfo.size = interSize;
				bufInfo.flags = 0;
				ensaviTest.writeVideoSample(bufData, bufInfo);
				sampler.videoCount++;
				sampler.videoBytes += bufInfo.size;

				bufInfo.presentationTimeUs = recordMils * 1000;
				bufInfo.size = audioSize;
				ensaviTest.writeAudioSample(bufData, bufInfo);
				sampler.audioCount++;
				sampler.audioBytes += bufInfo.size;
				recordMils += 20;

				bufInfo.presentationTimeUs = recordMils * 1000;
				bufInfo.size = audioSize;
				ensaviTest.writeAudioSample(bufData, bufInfo);
				sampler.audioCount++;
				sampler.audioBytes += bufInfo.size;
				recordMils += 20;

				sampler.setLastStamp(recordMils);
			}
		}



		static private void fillupBuffer(ByteBuffer aBuffer) {
			aBuffer.clear();
			for (int i=0; i<aBuffer.capacity(); i++)
				aBuffer.put( (byte) i);
			aBuffer.flip();
		}


	}




}