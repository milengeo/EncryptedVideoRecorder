
package com.rustero;

import android.util.Log;

import java.nio.ByteBuffer;

public class Savi2Lib {

    static {
        System.loadLibrary("Savi2Lib");
    }
    private static void dlog(String aLine) { Log.d("NATLIB", aLine); }

    // natlib common api
    public native int attach(String aPath);
    public native int selfTest(String aPath);

    public native String gep();
    public native int srd(String E, String K);



    // savi common api
    //public native String getSaviErrorText();
	public native int getSaviMethod();
    public native int getSaviLength(String aPath);


    // savi entracker api
    public native int createSaviWriter(String aPath);
    public native int createSaviEntracker(String aSkey);
    public native int deleteSaviEntracker();
    public native int startSavi();
    public native int finishSavi();

    public native int addVideoFormat(String aCode);
    public native int setVideoCsd0(int aSize, ByteBuffer aBybu);
    public native int setVideoCsd1(int aSize, ByteBuffer aBybu);
    public native int writeVideoSample(int aStamp, int aSize, ByteBuffer aBybu, boolean aSync);

    public native int addAudioFormat(String aCode);
    public native int setAudioCsd0(int aSize, ByteBuffer aBybu);
    public native int writeAudioSample(int aStamp, int aSize, ByteBuffer aBybu);


    // savi detracker api
    public native int createSaviPicker();
    public native int feedSaviPicker(ByteBuffer aBybu, int aSize);
    public native int getSaviPickerCount();
    public native int getSaviPickerSpare();
    public native int getSaviFeedRoom();

    public native int createSaviDetracker(String aSkey);
    public native int deleteSaviDetracker();

    public native int getSaviDuration();
    public native String getVideoFormat();
    public native int getVideoCsd0(ByteBuffer aBybu);
    public native int getVideoCsd1(ByteBuffer aBybu);

    public native String getAudioFormat();
    public native int getAudioCsd0(ByteBuffer aBybu);

    public native boolean endedSavi();
    public native int advanceSavi();
    public native long getSaviSeekLocus(int aStamp);
    public native long getSaviRosterLocus();
    public native int loadSaviRoster();

    public native int resetSavi();
    public native int getSaviSampleTrack();
    public native int getSaviSampleTime();
    public native int getSaviSampleSize();
    public native boolean isSyncSample();
    public native int readSaviSampleData(ByteBuffer aBybu);


}
