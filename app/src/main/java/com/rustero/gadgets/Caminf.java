
package com.rustero.gadgets;


import android.hardware.Camera;
import android.util.Log;

import com.rustero.App;

import java.util.List;

@SuppressWarnings("deprecation")




public class Caminf {

	public Caminf() {
	}


	public static int findFrontCamera() {
		int result = -1;
		Camera.CameraInfo info = new Camera.CameraInfo();
		try {
			int numCameras = Camera.getNumberOfCameras();
			for (int i = 0; i < numCameras; i++) {
				Camera.getCameraInfo(i, info);
				if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					result = i;
					break;
				}
			}
		} catch (Exception ex) {
			App.log(" *** ex Caminf_openFrontCamera" + ex.getMessage());
		}

		return result;
	}



	public static int findBackCamera() {
		int result = -1;
		Camera.CameraInfo info = new Camera.CameraInfo();
		try {
			int numCameras = Camera.getNumberOfCameras();
			for (int i = 0; i < numCameras; i++) {
				Camera.getCameraInfo(i, info);
				if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					result = i;
					break;
				}
			}
		} catch (Exception ex) {
			App.log(" *** ex Caminf_openFrontCamera" + ex.getMessage());
		}

		return result;
	}



    public static SizeList getCameraSizes(Camera aCamera) {
        SizeList result = new SizeList();

		Size2D maxiSize = new Size2D(1280, 720);
		Camera.Size prefSize = aCamera.getParameters().getPreferredPreviewSizeForVideo();
		if (prefSize != null) {
            App.log( "  * Camera preferred size for video is " + prefSize.width + "x" + prefSize.height);
			maxiSize = new Size2D(prefSize.width, prefSize.height);
		}

        List<Camera.Size> casis = getSupportedSizes(aCamera.getParameters());
        for (Camera.Size casi : casis) {
            App.log("Supported size: " + casi.width + "x" + casi.height);
			Size2D size2 = new Size2D(casi.width, casi.height);
			//check for maximum size
			if (size2.isAbove(maxiSize.x, maxiSize.y)) {
				continue;
			}
            result.addSize(size2);
			App.log("Gotten size: " + casi.width + "x" + casi.height);
        }

        return result;
    }



    public static List<Camera.Size> getSupportedSizes(Camera.Parameters aPars) {
        List<Camera.Size> casis = aPars.getSupportedVideoSizes();
        if (null == casis)
            casis = aPars.getSupportedPreviewSizes();
        return casis;
    }



	public static boolean hasFrameRate(Camera.Parameters aPars, int aFps1000) {
		boolean result = false;
		List<int[]> fpsList = aPars.getSupportedPreviewFpsRange();
		for (int i=0; i<fpsList.size(); i++) {
			int[] res2 = fpsList.get(i);
			if (res2.length == 2)
				if (res2[0] == aFps1000  &&  res2[1] == aFps1000) {
					result = true;
					break;
				}
		}

		return result;
	}


}
