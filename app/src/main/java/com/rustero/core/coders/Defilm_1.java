
package com.rustero.core.coders;


import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.Matrix;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.rustero.App;
import com.rustero.Errors;
import com.rustero.core.egl.glCore;
import com.rustero.core.egl.glScene;
import com.rustero.core.egl.glStage;
import com.rustero.core.egl.glSurface;
import com.rustero.core.egl.glTexture;
import com.rustero.gadgets.Size2D;
import com.rustero.savi2api.Desavi;
import com.rustero.gadgets.SinceMeter;
import com.rustero.gadgets.Fifo;
import com.rustero.gadgets.Size2D;
import com.rustero.gadgets.Tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("deprecation")



public class Defilm_1 extends DefilmA {

	private String mLastFault;
	private volatile boolean mPaused, mFullScreen;

	private SurfaceHolder mHolder;
	private Size2D mScreenSize = new Size2D();
	private Size2D mDecodeSize = new Size2D();
	private glSurface mDisplayGurface;

	private Engine mEngine;
	private Videco mVideco;
	private Audeco mAudeco;
	private Detrack mDetrack;

	private Fifo<Wave> mFifo = new Fifo(99);
	private long mDuramils;
	private long mLastTack, mKickMils = -1;
	private int mVideoTrack, mAudioTrack, mNextTrack;

	private int mVideoInputIndex = -1;
	private int mAudioInputIndex = -1;

	private ByteBuffer mSampleData = ByteBuffer.allocateDirect(1000000);
	private int mSampleSize;
	private long mSampleMils;
	private long mNextVideoMils;

	private boolean mCompleted;
	private MediaFormat mVideoFormat, mAudioFormat, mWaveFormat;
	private int mSeekMils, mPlayMils;
	private glStage mStage0, mStage1, mLastStage;
	private ExportEvents mExportEventer;
	private Desavi mDesavi;
	private SinceMeter mEndedMeter = new SinceMeter();
	private boolean mCamera90 = false;



	public Defilm_1(Desavi aDesavi) {
		mDesavi = aDesavi;
	}



	public void attachScreen(SurfaceHolder aHolder) {
		mHolder = aHolder;
		mScreenSize = new Size2D();
	}



	public void detachScreen() {
		mHolder = null;
	}



	public void changeScreen(int aWidth, int aHeight) {
		mScreenSize = new Size2D(aWidth, aHeight);
	}



	public void setExportEventer(ExportEvents aEventer) {
		mExportEventer = aEventer;
	}



	public void begin() {
		mLastFault = "";
		mSeekMils = -1;
		mPlayMils = 0;
		mPaused = false;
		mNextTrack = 0;
		mCompleted = false;

		mEngine = new Engine();
		mEngine.start();
	}


	public void cease() {
		mEngine.mQuit = true;
		while (!mEngine.mDone) {
			Tools.delay(1);
		}
		mEngine = null;
	}



	public void pause(boolean aPaused) {
		mKickMils = -1;
		if (aPaused) {
			mPaused = true;
		} else {
			mPaused = false;
		}
	}



	public void setFullScreen(boolean aFull) {
		mFullScreen = aFull;
	}



	public String getStatus() {
		String result = "";
		result += "  " + getDuramils();
		result += "  " + getPosiSecs();
		return result;
	}



	public int getDuramils() {
		int result = 0;
		result = (int) mDuramils / 1000;
		return result;
	}



	public int getPosiSecs() {
		int result = (mPlayMils) / 1000;
		return result;
	}



	public void seekSecs(int aSecs) {
		int mils = aSecs * 1000;
		mSeekMils = mils;
		mPlayMils = mils;
		App.log("seekSecs: " + aSecs);
	}



	public void setLastFault(String aFault) {
		mLastFault = aFault;
		if (null != sEventer)
			sEventer.onFault();
	}



	public boolean isFaulted() {
		return ((null != mLastFault) && (!mLastFault.isEmpty()));
	}



	public String getLastFault() {
		return mLastFault;
	}



	public boolean isPaused() {
		return mPaused;
	}



	private void endOfStream() {
		pause(true);
		mSeekMils = 0;
		mPlayMils = 0;
		mCompleted = true;
		if (null != sEventer)
			sEventer.onStopped();
		if (isExporting())
			mEngine.mQuit = true;
	}



	private boolean isScreen90() {
		if (mScreenSize == null) return false;
		if (mScreenSize.y < mScreenSize.x)
			return false;
		else
			return true;
	}





	private class Engine extends Thread {

		private volatile boolean mQuit, mDone;

		@Override
		public void run() {
			attach();
			digest();
			detach();
			mDone = true;
			App.log(" * Engine_run_exit");
		}



		private void attach() {
			try {
				mEndedMeter.reset();
				if (isExporting())
					mExportEventer.onBegan();

				if (mLastFault.isEmpty()) {
					try {
						mDetrack = new Detrack();
						mDetrack.attach();
						if (mVideoTrack < 0)
							setLastFault("No video track is found!");
					} catch (Exception ex) {
						setLastFault(ex.getMessage());
						App.log(" * ex_11: " + ex.getMessage());
					}
				}

				if (mLastFault.isEmpty()) {
					try {
						mVideco = new Videco();
						mVideco.attach();
					} catch (Exception ex) {
						setLastFault(ex.getMessage());
						App.log(" * ex_11: " + ex.getMessage());
					}
				}

				if (mLastFault.isEmpty()) {
					if ((mAudioTrack > -1) && !isExporting()) {
						try {
							mAudeco = new Audeco();
							if (!mAudeco.attach())
								mAudeco = null;
						} catch (Exception ex) {
							setLastFault(ex.getMessage());
							App.log(" * ex_22: " + ex.getMessage());
						}
					}
				}
			} catch (Exception ex) {
				App.log(" ***** ex_Engine_attach: " + ex.getMessage());
			}
		}



		private void detach() {
			App.log( "Defilm_1_detach_11");
			try {
				if (null != mDetrack) {
					mDetrack.detach();
					mDetrack = null;
				}

				if (null != mVideco) {
					mVideco.detach();
					mVideco = null;
				}

				if (null != mAudeco) {
					mAudeco.detach();
					mAudeco = null;
				}

				if (isExporting())
					mExportEventer.onEnded(mCompleted);
			} catch (Exception ex) {
				App.log(" ***** ex_Engine_detach: " + ex.getMessage());
			}
			App.log( "Defilm_1_detach_99");
		}



		private void digest() {
			App.log( "Defilm_1_digest_11");
			try	{
				while (!mQuit) {
					Thread.sleep(1);
					if (!mLastFault.isEmpty()) break;
					if (null == sEventer) continue;

					if (null == mHolder) {
						mVideco.detachDisplay();
						continue;
					}
					if (null == mDisplayGurface)
						mVideco.attachDisplay();
					if (null == mDisplayGurface)
						continue;

					if (mPaused) continue;

					if (mSeekMils > -1) {
						mVideco.flush();
						if (null != mAudeco)
							mAudeco.flush();
						mDesavi.seekto(mSeekMils);
						mSeekMils = -1;
					}

					if (mDesavi.pumpFeed() < 0) {
						break;  // error
					}
					if (mDesavi.isHungry()) {
						continue;
					}

					mDetrack.digest();
					mVideco.digest();
					if (null != mAudeco)
						mAudeco.digest();

					if (mills() - mLastTack > 200) {
						mLastTack = mills();
						if (null != sEventer)
							sEventer.onProgress();
					}
				}

			} catch (Exception ex) {
				App.log(" ***** ex_Engine_digest: " + ex.getMessage());
			}
			App.log( "Defilm_1_digest_99");
		}
	}







	private class Detrack {


		private void attach() {
			mVideoTrack = -1;
			mAudioTrack = -1;
			mCamera90 = false;
			try {
				mDuramils = mDesavi.getDuration();
				mVideoFormat = mDesavi.getVideoFormat();
				mVideoTrack = mDesavi.getVideoTrack();
				if (mVideoTrack < 0) {
					setLastFault("There is no video track");
					App.log( "There is no video track");
					return;
				}
				int cols = mVideoFormat.getInteger("width");
				int rows = mVideoFormat.getInteger("height");
				mDecodeSize = new Size2D(cols, rows);
				if (mVideoFormat.containsKey("turn90"))
					mCamera90 = true;
				if (null != sEventer)
					sEventer.onFrameSize();

				mAudioFormat = mDesavi.getAudioFormat();
				mAudioTrack = mDesavi.getAudioTrack();
				if (mAudioTrack < 0) {
					App.log( "Audio track is not found.");
				} else if (isExporting())
					mExportEventer.onAudioFormat(mAudioFormat);

				mSeekMils = 0;
			} catch (Exception ex) {
				setLastFault(ex.getMessage());
				App.log( "* ex_attachDesavi: " + ex.getMessage());
			}
		}



		private void detach() {
		}



		private void digest() {
			try {
//				if (mSeekMils > -1) {
//					mVideco.flush();
//					if (null != mAudeco)
//						mAudeco.flush();
//					mDesavi.seekto(mSeekMils);
//					mSeekMils = -1;
//				}

				mNextTrack = mDesavi.getSampleTrack();
				if (mNextTrack < 0) {
					//App.log("Detracker_digest: nextTrack < 0");
					long enmi = mEndedMeter.sinceMils();
					if (enmi > 555)
						endOfStream();
					return;
				}

				if (mNextTrack == mVideoTrack) {
					if (!mVideco.needVideoSample()) {
						return;
					}
				} else if (mNextTrack == mAudioTrack) {
					if (null != mAudeco)
						if (!mAudeco.needAudioSample()) {
							return;
						}
				} else {
					int result = mDesavi.advance();
					if (result < 0)
						setLastFault(Errors.getText(result));
					return;
				}

				mSampleMils = mDesavi.getSampleTime();
				if (mSampleMils < 0) return;

				mSampleSize = mDesavi.readSampleData(mSampleData);
				mDesavi.advance();

				mSampleData.limit(mSampleSize);
				mSampleData.position(0);

				if (mNextTrack == mVideoTrack) {
					mVideco.feedVideoInput();
					if (isExporting())
						mExportEventer.onVideoSample(mSampleData, mSampleSize, mSampleMils*1000);
				}
				else if (mNextTrack == mAudioTrack) {
					if (isExporting())
						mExportEventer.onAudioSample(mSampleData, mSampleSize, mSampleMils*1000);
					else if (null != mAudeco)
						mAudeco.feedAudioInput();
				}

			} catch (Exception ex) {
				App.log( " ***** ex_Detrack_run: " + ex.getMessage());
				cease();
			}
		}
	}








	private class Videco {
		private int mVideoOutputIndex;
		private MediaCodec mVideoCodec;
		private boolean mImageReady;
		private MediaCodec.BufferInfo mVideoBufInfo = new MediaCodec.BufferInfo();

		private glTexture mCameraTexture;
		private SurfaceTexture mCameraSurtex;  // receives the output from the decoder
		private glSurface mHiddenSurface;

		private glScene mYuvScene;    // first scene
		private glScene mScreenScene;    // final scene

		private Size2D mStageSize = new Size2D();



		public Videco() {
			super();
		}



		private boolean isReady() {
			boolean result = (null == mDisplayGurface);
			return result;
		}



		private void flush() {
			mVideoCodec.flush();
			mVideoInputIndex = -1;
			mVideoOutputIndex = -1;
		}



		private void attach() {
			try {
				attachEgls();
				attachDisplay();
				attachDecor();
			} catch (Exception ex) {
				App.log(" * ex_Videco_attach: " + ex.getMessage());
			}
		}


		private void detach() {
			try {
				detachEgls();
				detachDisplay();
				detachDecor();
				glCore.delete();
			} catch (Exception ex) {
				App.log(" * ex_Videco_detach: " + ex.getMessage());
			}
		}



		private void attachEgls() {

			try {
				resizeStages(new Size2D(8, 8));

				glCore.create();
				mHiddenSurface = new glSurface(glCore.get(), 8, 8);
				mHiddenSurface.makeCurrent();

				mYuvScene = glScene.create(true);
				mScreenScene = glScene.create(false);

			} catch (Exception ex) {
				App.log(" * ex_Videco_attachEgls: " + ex.getMessage());
			}
		}


		private void detachEgls() {
			try {
				if (mHiddenSurface != null) {
					mHiddenSurface.release();
					mHiddenSurface = null;
				}

				glScene.delete(mYuvScene);
				glScene.delete(mScreenScene);

				glStage.delete(mStage0);
				glStage.delete(mStage1);

			} catch (Exception ex) {
				App.log(" * ex_Videco_detachEgls: " + ex.getMessage());
			}
		}



		private void attachDisplay() {
			try {
				if (null != mDisplayGurface) return;
				if (null == mHolder) return;
				if (mScreenSize.isZero()) return;

				Surface surface = mHolder.getSurface();
				if (null == surface) return;

				mDisplayGurface = new glSurface(glCore.get(), surface);
				mDisplayGurface.makeCurrent();

//				mDisplaySize = new Size2(mScreenSize);
			} catch (Exception ex) {
				App.log(" ***_ex attachDisplay: " + ex.getMessage());
			}
		}


		private void detachDisplay() {
			try {
				if (mDisplayGurface != null) {
					mDisplayGurface.release();
					mDisplayGurface = null;
				}
			} catch (Exception ex) {
				App.log(" * ex_Videco_detachDisplay: " + ex.getMessage());
			}
		}



		private void attachDecor() {
			if (mVideoTrack < 0) return;
			try {
				mCameraTexture = glTexture.create(true);
				mCameraSurtex = new SurfaceTexture(mCameraTexture.id);
				mCameraSurtex.setOnFrameAvailableListener(new SurfaceTextureListener());
				Surface surface = new Surface(mCameraSurtex);

				MediaFormat format = createVideoFormat();
				if (isExporting())
					mExportEventer.onVideoFormat(format);

				String mime = mVideoFormat.getString(MediaFormat.KEY_MIME);
				mVideoCodec = MediaCodec.createDecoderByType(mime);
				mVideoCodec.configure(format, surface, null, 0);

				mVideoCodec.start();
				surface.release();
			} catch (Exception ex) {
				setLastFault(ex.getMessage());
				App.log(" ***** ex_Videco_attachDecor: " + ex.getMessage());
			}
		}


		private void detachDecor() {
			try {
				if (mCameraTexture != null) {
					mCameraTexture.release();
					mCameraTexture = null;
				}

				if (mVideoCodec != null) {
					mVideoCodec.stop();
					mVideoCodec.release();
					mVideoCodec = null;
				}
			} catch (Exception ex) {
				App.log(" * ex_Videco_detachDecor: " + ex.getMessage());
			}
		}



		// "mime=video/avc", "width", "height", "csd-0", "csd-1"

		private MediaFormat createVideoFormat() {
			MediaFormat format = new MediaFormat();

			String mime = mVideoFormat.getString("mime");
			format.setString("mime", mime);

			int width = mVideoFormat.getInteger("width");
			format.setInteger("width", width);

			int height = mVideoFormat.getInteger("height");
			format.setInteger("height", height);

			ByteBuffer bb0 = mVideoFormat.getByteBuffer("csd-0");
			if (null != bb0) {
				ByteBuffer csd_0 = Tools.copyDirect(bb0);
				format.setByteBuffer("csd-0", csd_0);
			}

			ByteBuffer bb1 = mVideoFormat.getByteBuffer("csd-1");
			if (null != bb1) {
				ByteBuffer csd_1 = Tools.copyDirect(bb1);
				format.setByteBuffer("csd-1", csd_1);
			}

			return format;
		}





		private void drawFrame() {
			try {
				resizeStages(mDecodeSize);

//				if (mFullScreen)
//					mScreenScene.fillAspect(mDecodeSize, mScreenSize);
//				else
//					mScreenScene.cropAspect(mDecodeSize, mScreenSize);

				// latch the next frame from the camera
				mCameraSurtex.updateTexImage();
				mCameraSurtex.getTransformMatrix(mYuvScene.texMatrix);

				if (null != mDisplayGurface) {
					mDisplayGurface.makeCurrent();

					glCore.setViewport(mStageSize);
					glCore.get().clearScreen(0, 0, 0);
					mYuvScene.sourceTexid = mCameraTexture.id;
					mYuvScene.outputStage = mStage0;
					mYuvScene.draw();
					mLastStage = mStage0;

					Matrix.setIdentityM(mScreenScene.mvpMatrix, 0);
					if (mCamera90) {
						if (isScreen90()) {
							mScreenScene.cropAspect(mDecodeSize, mScreenSize);
							mScreenScene.rotateUpscale(mDecodeSize, -90);
							float screenAspect = 1f * mScreenSize.y / mScreenSize.x;
							Matrix.scaleM(mScreenScene.mvpMatrix, 0, screenAspect, screenAspect, 1f);
						} else
							mScreenScene.cropAspect(mDecodeSize, mScreenSize);
					} else {
						mScreenScene.cropAspect(mDecodeSize, mScreenSize);
					}

					glCore.setViewport(mScreenSize);
					glCore.get().clearScreen(0, 0, 0);
					mScreenScene.sourceTexid = mLastStage.getTextureId();
					mScreenScene.draw();

					mDisplayGurface.swapBuffers();
				}
			} catch (Exception ex) {
				App.log(" ***** ex_MultiEncor_drawFrame: " + ex.getMessage());
				cease();
			}
		}



		private void digest() {
			try {
				if (mVideoOutputIndex < 0)
					pullVideoOutput();

				if (mVideoOutputIndex >= 0) {
					boolean itisTime;
					if (isExporting())
						itisTime = true;
					else
						itisTime = timeForVideo(mNextVideoMils);
					if (itisTime) {
						if (!mPaused)
							mPlayMils = (int) mNextVideoMils;
						mVideoCodec.releaseOutputBuffer(mVideoOutputIndex, true);
						mVideoOutputIndex = -99;
						//App.log( "  mPlayMils: " + mPlayMils);
					}
				}

				if (mImageReady) {
					mImageReady = false;
					drawFrame();
				}
			} catch (Exception ex) {
				App.log(" ***** ex_Videco_digest: " + ex.getMessage());
			}
		}



		public boolean needVideoSample() {
			if (mVideoInputIndex >= 0) return true;
			mVideoInputIndex = mVideoCodec.dequeueInputBuffer(0);
			return (mVideoInputIndex >= 0);
		}



		private void feedVideoInput() {
			if (mVideoInputIndex < 0) return;
			long took = System.currentTimeMillis();

			ByteBuffer bybu = mVideoCodec.getInputBuffers()[mVideoInputIndex];
			if (null == bybu) return;
			try {
				Tools.deepCopy(mSampleData, bybu);
				//mFeedMils = (int) mSampleMils;
				long sampleMics = mSampleMils * 1000;
				mVideoCodec.queueInputBuffer(mVideoInputIndex, 0, mSampleSize, sampleMics, 0);
				//App.log( "  mFeedMils: " + mFeedMils);
			} catch (Exception ex) {
				App.log("ex_feedVideoInput: " + ex.getMessage());
			}
			mVideoInputIndex = -1;

			took = System.currentTimeMillis() - took;
			if (took > 22) App.log("### feedVideoInput " + took);
		}



		private void pullVideoOutput() {
			long took = System.currentTimeMillis();
			try {
				mVideoOutputIndex = mVideoCodec.dequeueOutputBuffer(mVideoBufInfo, 0);
				if (mVideoOutputIndex >= 0) {
					mNextVideoMils = mVideoBufInfo.presentationTimeUs / 1000;
					//App.log("pullVideoOutput: " + mNextVideoMils);

				} else if (mVideoOutputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
					//App.log( "no output from mVideco available");
				} else if (mVideoOutputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					//App.log( "mVideco output buffers changed");
				} else if (mVideoOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					MediaFormat format = mVideoCodec.getOutputFormat();
				}
			} catch (Exception ex) {
				App.log(" * ex_pullVideoOutput: " + ex.getMessage());
				setLastFault("Data error!");
			}
			took = System.currentTimeMillis() - took;
			//if (took > 5) Log.i(LOG_TAG, "### pullVideoOutput " + took);
		}



		private boolean timeForVideo(long aPresMils) {
			if (mKickMils < 0) {
				App.log(" * kicking off");
				mKickMils = System.currentTimeMillis() - aPresMils;
			}
			long playMils = mKickMils + aPresMils;
			long delta = System.currentTimeMillis() - playMils;
			if (Math.abs(delta) > 555) {
				App.log(" * video resync: " + delta);
				mKickMils = System.currentTimeMillis() - aPresMils;
				delta = 0;
			}
			if (delta >= 0)
				return true;
			else
				return false;
		}



		private void resizeStages(Size2D aSize) {
			if (mStageSize.equals(aSize)) return;
			if (aSize.isZero()) return;
			mStageSize = new Size2D(aSize);

			glStage.delete(mStage0);
			mStage0 = new glStage(mStageSize);

			glStage.delete(mStage1);
			mStage1 = new glStage(mStageSize);
		}



		private class SurfaceTextureListener implements SurfaceTexture.OnFrameAvailableListener {
			@Override   // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
			public void onFrameAvailable(SurfaceTexture surfaceTexture) {
				mImageReady = true;
			}
		}


	}








	private class Audeco {

		Auplay mAuplay;
		private int mAudioOutputIndex;
		private MediaCodec.BufferInfo mBufInfo = new MediaCodec.BufferInfo();
		private MediaCodec mAudioCodec;
		//private ByteBuffer[] mAudioBuffers;



		// "mime=audio/mp4a-latm", "channel-count", "csd-0"
		MediaFormat createAudioFormat() {
			MediaFormat format = new MediaFormat();

			String mime = mAudioFormat.getString("mime");
			///if (mime.isEmpty())	mime = "audio/mp4a-latm";
			format.setString("mime", mime);

			int channelCount = mAudioFormat.getInteger("channel-count");
			format.setInteger("channel-count", channelCount);

			int sampleRate = mAudioFormat.getInteger("sample-rate");
			format.setInteger("sample-rate", sampleRate);

			ByteBuffer bb0 = mAudioFormat.getByteBuffer("csd-0");
			if (null != bb0) {
				ByteBuffer csd_0 = Tools.copyDirect(bb0);
				format.setByteBuffer("csd-0", csd_0);
			}

			return format;
		}


		private void flush() {
			mAudioCodec.flush();
			mAudioInputIndex = -1;
			mAudioOutputIndex = -1;
		}


		private boolean attach() {
			boolean result = true;
			try {
				MediaFormat format = createAudioFormat();
				mAuplay = new Auplay();

				String mime = format.getString(MediaFormat.KEY_MIME);
				mAudioCodec = MediaCodec.createDecoderByType(mime);
				mAudioCodec.configure(format, null, null, 0);

				mAudioCodec.start();
			} catch (Exception ex) {
				result = false;
				App.log( " ***** ex_Audeco_attach: " + ex.getMessage());
			}
			return result;
		}



		private void detach() {
			if (null != mAuplay)
				mAuplay.quit();
			if (mAudioCodec != null) {
				mAudioCodec.stop();
				mAudioCodec.release();
				mAudioCodec = null;
			}
		}



		private void digest() {
			try {
				pullAudioFrame();
				if (mAudioOutputIndex >= 0) {
					postAudioFrame();
					mAudioCodec.releaseOutputBuffer(mAudioOutputIndex, false);
					mAudioOutputIndex = -99;
				}
			} catch (Exception ex) {
				App.log( " *** ex_Audeco_digest: " + ex.getMessage());
			}
		}



		private boolean needAudioSample() {
			if (mAudioInputIndex >= 0) return true;
			mAudioInputIndex = mAudioCodec.dequeueInputBuffer(0);
			return (mAudioInputIndex >= 0);
		}



		private void feedAudioInput() {
			if (mKickMils < 0) return;  //wait for kick off
			if (mAudioInputIndex < 0) return;
			ByteBuffer bybu = mAudioCodec.getInputBuffers()[mAudioInputIndex];
			if (null == bybu) return;
			long took = System.currentTimeMillis();
			try {
				Tools.deepCopy(mSampleData, bybu);
				long sampleMics = mSampleMils * 1000;
				mAudioCodec.queueInputBuffer(mAudioInputIndex, 0, mSampleSize, sampleMics, 0);
				//App.log( "  feedAudioInput: " + mSampleMics/1000);

			} catch (Exception ex) {
				App.log( " *** ex_feedAudioInput: " + ex.getMessage());
			}
			mAudioInputIndex = -1;

			took = System.currentTimeMillis() - took;
			if (took > 22) App.log( "### feedAudioInput " + took);
		}



		private void pullAudioFrame() {
			long took = System.currentTimeMillis();
			try {
				mAudioOutputIndex = mAudioCodec.dequeueOutputBuffer(mBufInfo, 0);
				if (mAudioOutputIndex >= 0) {

					if ((mBufInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						App.log("Audio BUFFER_FLAG_END_OF_STREAM: " + mBufInfo.presentationTimeUs / 1000);
						mAudioCodec.releaseOutputBuffer(mAudioOutputIndex, false);
						mAudioOutputIndex = -99;
///						endOfStream();
					} else {
						//App.log("pullAudioFrame: " + mBufInfo.presentationTimeUs/1000);
					}

				} else if (mAudioOutputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
				} else if (mAudioOutputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				} else if (mAudioOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					mWaveFormat = mAudioCodec.getOutputFormat();
					mAuplay.start();
					App.log( " * pullAudioFrame output format changed: " + mWaveFormat);
				} else {
					App.log( " *** unexpected result from mAudioCodec.dequeueOutputBuffer: " + mAudioOutputIndex);
				}
			} catch (Exception ex) {
				App.log( " ***** ex pullAudioFrame: " + ex.getMessage());
			}
			took = System.currentTimeMillis() - took;
			//if (took > 5) Log.i(LOG_TAG, "### pullAudioFrame " + took);
		}



		private void postAudioFrame() {
			long presMils = mBufInfo.presentationTimeUs / 1000;
			try {
				long playMils = mKickMils + presMils;
				long delta = System.currentTimeMillis() - playMils;
				Wave wave = new Wave(playMils, mBufInfo.size/2);
				wave.data = new short[mBufInfo.size/2];
				ByteBuffer outBuf = mAudioCodec.getOutputBuffers()[mAudioOutputIndex];
				outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(wave.data);

				mFifo.push(wave);
			} catch (Exception ex) {
				App.log( " *** ex postAudioFrame: " + ex.getMessage());
			}
		}

	}







	private class Auplay extends Thread {

		private volatile boolean mQuit;
		private Wave mWave;
		private AudioTrack mWavePlayer;
		private int mTotalBytes;
		private int mMinBufSize, mSampleRate;


		private void attach() {
			try {
				int channelCount, channelConfig;
				channelCount = mWaveFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
				channelConfig = AudioFormat.CHANNEL_OUT_MONO;
				switch (channelCount) {
					case 2:
						channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
						break;
					case 3:
						channelConfig = AudioFormat.CHANNEL_OUT_STEREO | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
						break;
					case 4:
						channelConfig = AudioFormat.CHANNEL_OUT_QUAD;
						break;
					case 5:
						channelConfig = AudioFormat.CHANNEL_OUT_QUAD | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
						break;
					case 6:
						channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
						break;
					case 7:
						channelConfig = AudioFormat.CHANNEL_OUT_5POINT1 | AudioFormat.CHANNEL_OUT_BACK_CENTER;
						break;
					case 8:
						channelConfig = AudioFormat.CHANNEL_OUT_7POINT1_SURROUND;
						break;
				}

				mSampleRate = mWaveFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
				mMinBufSize = 2*AudioTrack.getMinBufferSize(mSampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

				mWavePlayer = new AudioTrack(AudioManager.STREAM_MUSIC,
						mSampleRate,
						channelConfig,
						AudioFormat.ENCODING_PCM_16BIT,
						mMinBufSize,
						AudioTrack.MODE_STREAM);

			} catch (Exception ex) {
				setLastFault(ex.getMessage());
				App.log( " *** ex_Auplay_run: " + ex.getMessage());
			}
		}



		private void detach() {
			if (mWavePlayer != null) {
				mWavePlayer.setStereoVolume(0f, 0f);
				mWavePlayer.stop();
				mWavePlayer.release();
				mWavePlayer = null;
			}
		}



		private void quit() {
			mQuit = true;
		}



		private void hold() {
			if (null == mWavePlayer) return;
			mWavePlayer.setStereoVolume(0f, 0f);
			mWavePlayer.pause();
			mWavePlayer.flush();
			mTotalBytes = 0;
			App.log( " * mWavePlayer.pause()");
		}



		@Override
		public void run() {
			if (null == mWaveFormat) return;

			attach();
			mWavePlayer.pause();

			try {
				while (!mQuit) {
					Thread.sleep(1);
					pumpWave();
				}
			} catch (Exception ex) {
				App.log( " * ex_Auplay_run: " + ex.getMessage());
			}

			detach();
			App.log( " * Auplay_exit");
		}



		private void pumpWave() {

			int playState = mWavePlayer.getPlayState();
			if (playState == AudioTrack.PLAYSTATE_PLAYING) {
				if (mPaused)
					hold();
			}

			if (null == mWave) {
				mWave = mFifo.pull();
				if (mPaused)
					mWave = null;
				if (null != mWave) {
					if (playState != AudioTrack.PLAYSTATE_PLAYING) {
						mWavePlayer.play();
						//App.log("mWavePlayer.play()");
					}
				}
			}

			if (null == mWave) return;
			mTotalBytes += mWave.size;

			if (mTotalBytes < mMinBufSize) {
				mWavePlayer.setStereoVolume(0f, 0f);
			} else {
				mWavePlayer.setStereoVolume(1f, 1f);
			}

			try {
				long delta = System.currentTimeMillis() - mWave.mils;
				//App.log( String.format("pumpWave: %d,  %d", delta, mWave.size));

				if (delta < -999) {
					App.log( " * very fast audio, throw away: " + delta);
					mWave = null;
					hold();
					return;
				}

				if (delta > 222) {
					App.log( " * late audio, skip it: " + delta);
					mWave = null;
					hold();
					return;
				}

				if (delta < 0) {
					//App.log( " * early audio, wait: " + delta);
					return;
				}

				int count = mWavePlayer.write(mWave.data, 0, mWave.size);
				mWave = null;

			} catch (Exception ex) {
				App.log( " *** ex_pumpWave: " + ex.getMessage());
			}
		}
	}



	private class Wave {
		long mils;
		int size;
		short[] data;

		Wave(long aMils, int aSize) {
			mils = aMils;
			size = aSize;
		}
	}






	private long mills() {
		return System.currentTimeMillis();
	}



	private boolean isExporting() {
		return (null != mExportEventer);
	}



}
