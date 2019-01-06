package com.rustero.core.coders;


import android.view.SurfaceHolder;

import com.rustero.core.stickers.StickerObject;
import com.rustero.gadgets.Size2D;

import java.util.List;

public abstract class EnfilmA {

	public static final int STATE_IDLE 			= 0;
	public static final int STATE_STARTING 		= 1;
	public static final int STATE_RUNNING 		= 2;
	public static final int STATE_RECORDING 	= 3;

	public interface Events {
		void onStateChanged();
		void onProgress();
		void onFault(final String aMessage);
	}

	public class StatusC {
		public void reset() {
			name = "";
			secs = 0; size = 0;	rate = 0;
		}
		public String name;
		public long secs, size;
		public int rate;
	}




	protected static EnfilmA self;
	protected volatile int mState = EnfilmA.STATE_IDLE;
	protected volatile StatusC mStatus = new StatusC();




	public static EnfilmA get() {
		create();
		return self;
	}


	public static void create() {
		if (null != self) return;
		self = new Enfilm_1();
	}


	public static void delete() {
		if (null == self) return;
		self = null;
	}


	public abstract void attachEngine(Events aEventer);
	public abstract void detachEngine();
	public abstract void changeScreen(int aWidth, int aHeight);
	public abstract void attachScreen(SurfaceHolder aHolder);
	public abstract void detachScreen();
	public abstract String getOutputName();
	public abstract boolean isRecording();
	public abstract void begin();
	public abstract void cease();
	public abstract float getZoom();
	public abstract void applyZoom();
	public abstract void incZoom();
	public abstract void decZoom();

	public abstract boolean hasFlash();
	public abstract void turnFlash(boolean aOn);

	public abstract void setEffects(List<String> aList);
	public abstract void setTheme(StickerObject aTheme);
	public abstract void setGrid(boolean aEnabled);

	public abstract Size2D getCameraSize();
	public abstract int getState();

	public abstract void resetStatus();
	public abstract StatusC getStatus();

}
