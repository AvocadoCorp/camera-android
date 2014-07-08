package io.avocado.camera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.Touch;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CameraActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;

    private static final String TAG = "CameraActivity";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private boolean isRecording = false;
    private static int currentCameraId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //Create an instance of camera
        mCamera = getCameraInstance();
        Log.d("testing", "camera null? " + String.valueOf(mCamera == null));

        //Create a Preview view and set it as content for the activity
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        //make another imageview
        //Take photo
        ImageView cameraButton = (ImageView) findViewById(R.id.button_photo);
        cameraButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        //get an image from the camera
                        mCamera.startPreview();
                        mCamera.takePicture(null, null, mPicture);



                    }

                }

        );
        //Add a listener to the video imageview
        //this imageview records video
        ImageView videoButton = (ImageView) findViewById(R.id.button_video);
        videoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //recording
                        Log.d("testing", "video is triggered" + isRecording);
                        if (isRecording) {
                            //stop recording and release camera
                            mMediaRecorder.stop();
                            releaseMediaRecorder();
                            mCamera.unlock();


                            //inform the user that recording has stopped
                            setVideoButtonText("Capture video");
                            isRecording = false;
                        } else {
                            // initialize video camera
                            if (prepareVideoRecorder()) {
                                //Camera is available and unlocked, MediaRecorder is prepared, now you can start recording
                                mMediaRecorder.start();

                                //inform the user that recording has started
                                //setVideoButtonText("Stop");
                                isRecording = true;
                            } else {
                                //prepare didn't work, release the camera
                                releaseMediaRecorder();
                                //inform user
                                Log.d(TAG, "MediaRecorder has been released");
                            }
                        }
                    }
                }
        );

        ImageView otherCamera = (ImageView) findViewById(R.id.button_otherCamera);
        otherCamera.setOnClickListener(
                new View.OnClickListener(){
            @Override
                public void onClick(View v){
                //release camera before switching otherwise, app will crash
                mCamera.release();
                //swap the id of the camera to be used
                if(currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                    currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
                else {
                    currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                mCamera = Camera.open(currentCameraId);
                setCameraDisplayOrientation(CameraActivity.this, currentCameraId, mCamera);
                try{
                    mCamera.setPreviewDisplay(mPreview.getmHolder());
                }
                catch(IOException e){
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }



        });

        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                cameraFocus();
                return true;
            }
        });


    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            //if device has a camera
            return true;
        } else {
            //if not
            return false;
        }
    }


    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            //Grab camera instance
            //Camera.getNumberOfCameras()-1

            Log.d("Number of Cameras", String.valueOf(Camera.getNumberOfCameras()));
            int numCam = Camera.getNumberOfCameras()-1;
            currentCameraId = numCam;
            Log.d("testing", "numCam: " + numCam);
            c = Camera.open(numCam);
            Log.d("testing", "is camera null? " + String.valueOf(c == null));
        } catch (Exception e) {
            //Camera is not available (in use or does not exist)
            Log.d("testing", "caught exception: " + e);
        }
        return c; //returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {

                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }

    };


    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    private boolean prepareVideoRecorder() {

        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.lock();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(mCamera.getNumberOfCameras() - 1, CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void setPhotoButtonText(String text) {
        ImageView photoButton = (ImageView) findViewById(R.id.button_photo);
        //photoButton.setText(text);
    }

    private void setVideoButtonText(String text) {
        ImageView videoButton = (ImageView) findViewById(R.id.button_video);
        //videoButton.setText(text);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void cameraFocus() {


        // set Camera parameters
        Camera.Parameters params = mCamera.getParameters();

        if (params.getMaxNumMeteringAreas() > 0) {
            // check that metering areas are supported
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
            Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image
            meteringAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%
            Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image
            meteringAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%
            params.setMeteringAreas(meteringAreas);
            Log.d("cameraFocus is called upon", "YES");

        }

        mCamera.setParameters(params);

    }


}
