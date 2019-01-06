
package com.rustero.core.coders;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.rustero.App;

import java.io.File;
import java.nio.ByteBuffer;


public class Enmuxer {


	private final int TRACK_NUMBER = 2;
	private File mFile;
	private volatile int mTrackCount;
	private int mAudioTrack, mVideoTrack;
    private MediaMuxer mMuxer;



    synchronized public void attach(String aPath) {
        mTrackCount = 0;
        try {
            mMuxer = new MediaMuxer(aPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }
        catch (Exception ex) {
            App.log(" * Tracker attach " + ex.getMessage());
        }
    }



    synchronized public void detach() {
            try {
                if (mMuxer != null) {
                    mTrackCount = 0;
                    mMuxer.stop();
                    mMuxer.release();
                    App.log("mMuxer.cease");
                    mMuxer = null;
                }
            } catch (Exception ex) {
                App.log(" *** Tracker detach " + ex.getMessage());
            }
    }



    synchronized public void addAudioTrack(MediaFormat aFormat) {
            try {
                if (mMuxer != null) {
                    if (aFormat != null)
                        mAudioTrack = mMuxer.addTrack(aFormat);
                    App.log(" * addAudioTrack " + mAudioTrack);
                    mTrackCount++;
                    if (mTrackCount == TRACK_NUMBER) {
                        mMuxer.start();
                    }

                }
            }
            catch (Exception ex) {
                App.log(" *** Tracker addAudioTrack " + ex.getMessage());
            }
    }



    synchronized public void addVideoTrack(MediaFormat aFormat) {
            try {
                if (mMuxer != null) {
                    if (aFormat != null)
                        mVideoTrack = mMuxer.addTrack(aFormat);
                    App.log(" * addVideoTrack " + mVideoTrack);
                    mTrackCount++;
                    if (mTrackCount == TRACK_NUMBER) {
                        mMuxer.start();
                    }
                }
            }
            catch (Exception ex) {
                App.log("addVideoTrack " + ex.getMessage());
            }
    }



    synchronized public boolean isStarted() {
		if (mMuxer == null) return false;
        return (mTrackCount == TRACK_NUMBER);
    }



    synchronized public void writeAudioSample(ByteBuffer aData, int aSize, long aMics) {
        if (!isStarted()) return;
            try {
				MediaCodec.BufferInfo bufInfo = new MediaCodec.BufferInfo();
				bufInfo.size = aSize;
				bufInfo.presentationTimeUs = aMics;

                mMuxer.writeSampleData(mAudioTrack, aData, bufInfo);
				//App.log("writeAudio " + aMics/1000);
            }
            catch (Exception ex) {
                App.log(" * EX writeAudio " + ex.getMessage());
            }
    }



    synchronized public boolean writeVideoSample(ByteBuffer aData, int aSize, long aMics) {
        if (!isStarted()) return true;
            try {
				MediaCodec.BufferInfo bufInfo = new MediaCodec.BufferInfo();
				bufInfo.size = aSize;
				bufInfo.presentationTimeUs = aMics;

				int pos = aData.position();
				if (aSize > 9) {
					byte[] ba = new byte[9];
					aData.get(ba);
					aData.position(pos);
					if (0==ba[0] && 0==ba[1] && 0==ba[2] && 1==ba[3]) {
						byte h = (byte)(ba[4] & 0x1f);
						if (h==5 || h==7)
							bufInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
					}
					else
						return false;
				}

                mMuxer.writeSampleData(mVideoTrack, aData, bufInfo);
				//App.log("writeVideo " + aMics/1000);
            }
            catch (Exception ex) {
                App.log("writeVideo " + ex.getMessage());
            }
		return true;
    }


}




