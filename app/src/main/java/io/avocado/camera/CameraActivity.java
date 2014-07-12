package io.avocado.camera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.graphics.BitmapFactory.Options;


public class CameraActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;

    private static final String TAG = "CameraActivity";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private boolean isRecording = false;
    private static int currentCameraId = 0;

    ImageView cameraButton;

    private long initTime;
    private MotionEvent lastEvent;
    private Handler handler = new Handler();

    private static int RESULT_LOAD_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //Create an instance of camera
        mCamera = getCameraInstance();
        Log.d("testing", "camera null? " + String.valueOf(mCamera == null));
        mPreview = new CameraPreview(this, mCamera);
        Log.d("testing", "preview null? " +String.valueOf(mPreview == null));

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        cameraButton = (ImageView) findViewById(R.id.button_photo);
        cameraButton.setOnTouchListener(
                new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View view, MotionEvent me) {
                        //user is clicking image view
                        if (me.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            //receive time of down press
                            initTime = System.currentTimeMillis();
                            lastEvent = me;
                            handler.postDelayed(runnable, 10);

                        }

                        return true;

                    }


                }
        );

        //this creates the image view to swap between cameras.
        ImageView otherCamera = (ImageView) findViewById(R.id.button_otherCamera);
        otherCamera.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //release camera before switching otherwise, app will crash
                        mCamera.release();
                        //swap the id of the camera to be used
                        if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                        } else {
                            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                        }
                        mCamera = Camera.open(currentCameraId);
                        setCameraDisplayOrientation(CameraActivity.this, currentCameraId, mCamera);
                        try {
                            mCamera.setPreviewDisplay(mPreview.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCamera.startPreview();
                    }


                }
        );

        //creates the image view to access gallery.
        ImageView imageGallery = (ImageView) findViewById(R.id.button_gallery);
        imageGallery.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });


    }



    //runnable class for detecting how long a user is holding the camera button.
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //check for state every .01 second
            if (lastEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                long curTime = System.currentTimeMillis();
                if (curTime - initTime >= (long) 500 && !isRecording) {
                    //change to video icon
                    //begin to take video
                    //initialize video camera
                    cameraButton.setImageResource(R.drawable.video);
                    if (prepareVideoRecorder()) {
                        //Camera is available and unlocked, MediaRecorder is prepared, now you can start recording

                        mMediaRecorder.start();
                        isRecording = true;
                        //inform user that recording has started

                    } else {
                        //prepare didn't work, release camera
                        releaseMediaRecorder();
                        //inform user
                        Log.d(TAG, "MediaRecorder has been released");
                    }
                }
            } else if (lastEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                if (isRecording) {
                    //stop recording
                    cameraButton.setImageResource(R.drawable.check);
                    mMediaRecorder.stop();
                    releaseMediaRecorder();
                    mCamera.unlock();
                    isRecording = false;
                    //inform the user that the recording has stopped
                    Log.d("recording finished", String.valueOf(isRecording == false));
                }
                long curTime = System.currentTimeMillis();
                if (curTime - initTime < (long) 500) {
                    // take photo
                    //get an image from the camera
                    mCamera.stopPreview();
                    mCamera.startPreview();
                    Log.d("camera null?", String.valueOf(mCamera == null));
                    mCamera.takePicture(null , null, mPicture);

                }
            }
            handler.postDelayed(this, 10);
        }
    };

    //gallery access
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null){
            Log.d("is loading", String.valueOf(true));
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            ImageView imageView = (ImageView) findViewById(R.id.imgView);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
        }

    }

    //sets the camera display orientation.
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
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
            int numCam = Camera.getNumberOfCameras() - 1;
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



    //will try to write storage for media.
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            /*File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
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
            }*/
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
            Options options = new Options();
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length, options);

            String path = saveToInternalStorage(image);
            loadImageFromStorage(path);


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

    //prepares the video recorder for use.
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
            Thread.sleep(1000);
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    //on pause will release both the camera and video recorder.
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    //releases video recorder.
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    //releases camera.
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mPreview.getHolder().removeCallback(mPreview);
        }
    }

    //possible internal storage method
    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File myPath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {

            fos = new FileOutputStream(myPath);

            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return directory.getAbsolutePath();
    }

    private void loadImageFromStorage(String path)
    {

        try {
            File f=new File(path, "profile.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            ImageView img=(ImageView)findViewById(R.id.imgPicker);
            img.setImageBitmap(b);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

    }



}
