package com.rustero.gadgets;


import android.app.Activity;
import android.hardware.Camera;

import com.rustero.App;
import com.rustero.R;

public class Mycams {


	static final public String CAMERA_LIST = "camera_list.xml";

	private static SizeList mFrontResoList = new SizeList();
	private static SizeList mBackResoList = new SizeList();

	private static final int PAID_MAX_SIZE = 722;
	private static final int FREE_MAX_SIZE = 606;

	public static int getMaxCameraHeight() {
		if (App.isPremium())
			return PAID_MAX_SIZE;
		else
			return FREE_MAX_SIZE;
	}


	public static void attach(Activity aActivity) {
		long took = App.mils();

		String code = Tools.readPrivateFile(App.self, CAMERA_LIST);
		if (code.isEmpty()) {
			try {
				findCameras();
			} catch (Exception ex){
				App.log(" * Cannot open camera, it may be used by another app.");
				App.finishActivityAlert(aActivity, "Error", "Cannot open camera, it may be used by another app.");
				return;
			}

		} else {
			Mycams.readCameras(code);
		}

		if ( 0==mFrontResoList.length()  &&  0==mBackResoList.length()) {
			App.finishActivityAlert(aActivity, App.self.getResources().getText(R.string.app_name).toString(), "No camera is found!");
		}

		took = App.mils() - took;
		App.log("loadCameras: " + took);
	}



	public static String getFrontResoText() {
		verifyResolution("front_resolution", mFrontResoList);
		String result = App.getPrefStr("front_resolution");
		return result;
	}



	public static String getBackResoText() {
		verifyResolution("back_resolution", mBackResoList);
		String result = App.getPrefStr("back_resolution");
		return result;
	}



	public static int getCameraCount() {
		if (hasFrontCamera() && hasBackCamera())
			return 2;
		else if (!hasFrontCamera() && !hasBackCamera())
			return 0;
		else return 1;
	}



	public static boolean hasFrontCamera() {
		return (mFrontResoList.length() > 0);
	}


	public static boolean hasBackCamera() {
		return (mBackResoList.length() > 0);
	}




	public static void verifySelectedResolutions() {
		verifyResolution("front_resolution", mFrontResoList);
		verifyResolution("back_resolution", mBackResoList);
	}


	public static String[] getFrontResoArray() {
		String[] result = loadResoArray(mFrontResoList);
		return result;
	}


	public static String[] getBackResoArray() {
		String[] result = loadResoArray(mBackResoList);
		return result;
	}



	private static void verifyResolution(String aName, SizeList aList) {
		if (aList.length() < 1) return;  // nothing to do
		String sizeText  = App.getPrefStr(aName);
		Size2D seldSize = Size2D.parseText(sizeText);
		if (seldSize.y <= getMaxCameraHeight()) return;  //it's ok

		// selected size is too large, we need to change it
		String[] array = loadResoArray(aList);
		if (array.length < 1) return;
		sizeText = array[array.length-1];
		App.setPrefStr(aName, sizeText);
	}




	private static String[] loadResoArray(SizeList aList) {
		// first, found the count
		int count = 0;
		for (int i=0; i<aList.length(); i++) {
			Size2D size2 = aList.get(i);
			if (size2.y > getMaxCameraHeight()) break;
			count++;
		}

		// second, build the array
		String[] result = new String[count];
		for (int i=0; i<count; i++) {
			Size2D size2 = aList.get(i);
			result[i] = size2.toText();
		}

		return result;
	}



	private static void findCameras() {
		// first run, detect cameras
		Lmx xml = new Lmx();
		int camId;
		Camera frontCamera = null;
		camId = Caminf.findFrontCamera();
		if (camId > -1)
			frontCamera = Camera.open(camId);
		if (null != frontCamera) {
			App.setPrefBln("now_front", true);
			mFrontResoList = Caminf.getCameraSizes(frontCamera);
			frontCamera.release();
			//App.log("front camera sizes");
			packCameraSizes(xml, mFrontResoList);
			xml.pushNode("front_sizes");

			Size2D s2 = mFrontResoList.getBelowHeight(500);
			String defres = s2.toText();
			App.setPrefStr("front_resolution", defres);
		}

		Camera backCamera = null;
		camId = Caminf.findBackCamera();
		if (camId > -1)
			backCamera = Camera.open(camId);
		if (null != backCamera) {
			App.setPrefBln("now_front", false);
			mBackResoList = Caminf.getCameraSizes(backCamera);
			backCamera.release();
			packCameraSizes(xml, mBackResoList);
			xml.pushNode("back_sizes");

			Size2D s2 = mBackResoList.getBelowHeight(500);
			String defres = s2.toText();
			App.setPrefStr("back_resolution", defres);
		}

		xml.pushNode("cameras");
		final String code = xml.getCode();
		Tools.writePrivateFile(App.self, CAMERA_LIST, code);

		readCameras(code);
	}



	private static void packCameraSizes(Lmx aXml, SizeList aList) {
		aList.sort();
		for (int i=0; i<aList.length(); i++) {
			Size2D size = aList.get(i);
			App.log("cam size: " + size.x + "x" + size.y);
			aXml.addInt("width", size.x);
			aXml.addInt("height", size.y);
			aXml.pushItem("item");
		}
	}



	private static void readCameras(String aCode) {

		try {
			Lmx xml = new Lmx(aCode);

			xml.pullNode("front_sizes");
			if (!xml.isEmpty()) {
				readCameraSizes(xml, mFrontResoList);
			}

			xml.pullNode("back_sizes");
			if (!xml.isEmpty()) {
				readCameraSizes(xml, mBackResoList);
			}
		} catch (Exception ex) {
			App.log("***_ex readCameras" + ex.getMessage());
		}
	}



	private static void readCameraSizes(Lmx aXml, SizeList aList) {
		aList.clear();
		while (true) {
			aXml.pullItem("item");
			if (aXml.isEmpty()) break;
			int width = aXml.getInt("width");
			int height = aXml.getInt("height");
			aList.addSize(new Size2D(width, height));
		}
	}




}
