package io.avocado.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by dylanwestover on 6/30/14.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    private static final String TAG = "CameraPreview";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        //Install a SurfaceHolder. Callback to get notified when the underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        //deprecated setting, following is required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /*public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }*/

   /* public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }*/


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Once the surface has been created, the camera is told where to draw the preview.
        try {
            Log.d("is mCamera null?", String.valueOf(mCamera == null));
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        //If the preview can change or rotate, those events can be taken care of here.
        //Preview must be stopped before resizing or reformatting.

        mCamera.stopPreview();
        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();

        if (mHolder.getSurface() == null) {
            //preview surface does not exist;
            return;
        }

        //stop the preview then make changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            //ignore: tried to stop a non-existent preview
        }

        //set preview size and make any resize, rotate or reformatting changes needed
        //start preview with new settings

        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    //Can leave this empty. Camera preview will be released in CameraActivity.

    }




}


