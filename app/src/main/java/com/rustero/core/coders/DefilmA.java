package com.rustero.core.coders;


import android.media.MediaFormat;
import android.view.SurfaceHolder;

import com.rustero.savi2api.Desavi;

import java.nio.ByteBuffer;


public abstract class DefilmA {


	public interface PlayEvents {
		void onFrameSize();
		void onDestroyed();
		void onProgress();
		void onStopped();
		void onFault();
	}


	public interface ExportEvents {
		void onBegan();
		void onEnded(boolean aCompleted);
		void onVideoFormat(MediaFormat aFormat);
		void onAudioFormat(MediaFormat aFormat);
		void onVideoSample(ByteBuffer aData, int aSize, long aMics);
		void onAudioSample(ByteBuffer aData, int aSize, long aMics);
	}


	public static PlayEvents sEventer = null;
	protected static DefilmA self;


	public static DefilmA get() {
		return self;
	}


	public static void create(int aMethod, Desavi aDesavi) {
		if (null != self) return;
		switch (aMethod) {
			case 1:
				self = new Defilm_1(aDesavi);
				break;
		}
	}


	public static void delete() {
		if (null == self) return;
		self = null;
	}



	public abstract void attachScreen(SurfaceHolder aHolder);
	public abstract void detachScreen();
	public abstract void changeScreen(int aWidth, int aHeight);
	public abstract void setExportEventer(ExportEvents aEventer);
	public abstract void begin();
	public abstract void cease();
	public abstract void pause(boolean aPaused);
	public abstract void setFullScreen(boolean aFull);
	public abstract String getStatus();
	public abstract int getDuramils();
	public abstract int getPosiSecs();
	public abstract void seekSecs(int aSecs);
	public abstract void setLastFault(String aFault);
	public abstract boolean isFaulted();
	public abstract String getLastFault();
	public abstract boolean isPaused();

}
