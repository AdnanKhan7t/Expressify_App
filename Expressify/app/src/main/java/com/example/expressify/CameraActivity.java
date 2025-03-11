package com.example.expressify;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.HashMap;


import org.opencv.android.CameraBridgeViewBase;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Map;


public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG="MainActivity";

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;

    private TextView emotionT;

    private DatabaseReference mDatabase;
    private String parentKey;

    private ImageView flipCamera;


   // private String mostFrequentValue;

    private facialExpressionRecognition facialExpressionRecognition;

    private int index = 0;


    public CameraActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);

        }

        setContentView(R.layout.activity_camera);


        mOpenCvCameraView = findViewById(R.id.frame_Surface);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        parentKey = "Person-" + mDatabase.push().getKey();


            // If it's the first time, generate a new parent key

            // Save the parent key to SharedPreferences


        emotionT = findViewById(R.id.emotion);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        flipCamera = findViewById(R.id.flip_button);
        mOpenCvCameraView.setCameraIndex(0);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();

        try {
            int inputSize = 48;
            facialExpressionRecognition = new facialExpressionRecognition(getAssets(),CameraActivity.this,
                    "model300.tflite",inputSize);



        }catch (IOException e){
            e.printStackTrace();
        }

        flipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                swapCamera();



            }
        });

        Button StopButton = (Button) findViewById(R.id.stop_button);
        StopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenCvCameraView.disableView();
                retrieveDataAndFindMostFrequentValue(parentKey, new OnMostFrequentValueListener() {
                    @Override
                    public void onMostFrequentValue(String mostFrequentValue) {

                        if (mostFrequentValue != null) {
                            // Do something with the most frequent value
                            showToast("Most frequent value: " + mostFrequentValue);

                        } else {
                            showToast("No data found under parent key: " + parentKey);
                        }

                        startActivity(new Intent(CameraActivity.this,PredictionActivity.class).putExtra("mostFreq",mostFrequentValue));
                    }
                });


                //Toast.makeText(getApplicationContext(),"Stopped Camera",Toast.LENGTH_SHORT).show();


            }
        });


    }

    private void swapCamera() {
        index = index^1;
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(index);
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCvCameraView.setCameraPermissionGranted();  // <------ THIS!!!
                } else {
                    // permission denied
                }
                return;
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();

        if (index == 1){
            Core.flip(mRgba,mRgba,-1);
            Core.flip(mGray,mGray,-1);
        }



        mRgba = facialExpressionRecognition.recognizeImage(mRgba,emotionT);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String emotion = facialExpressionRecognition.getEmotion_s();
                emotionT.setText(emotion);
                String parentkey = saveDataToDatabase(emotion);
                //retrieveDataAndFindMostFrequentValue(parentkey);


               // rootDatabase.child(emotion);
            }
        });

        return mRgba;

    }

    private String saveDataToDatabase(String data) {
        // Generate a new key for the child using push()

        // Generate a new key for the child node using push()
        String childKey = mDatabase.child(parentKey).push().getKey();

        // Create a map to hold the data
        Map<String, Object> newData = new HashMap<>();
        newData.put(childKey, data);

        // Save data to the database
        mDatabase.child(parentKey).updateChildren(newData);
        return parentKey;
    }

    private void retrieveDataAndFindMostFrequentValue(String parentKey, final OnMostFrequentValueListener listener) {
        mDatabase.child(parentKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Integer> valueCounts = new HashMap<>();

                // Loop through the children and count the occurrences of each value
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String value = childSnapshot.getValue(String.class);
                    if (value != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            valueCounts.put(value, valueCounts.getOrDefault(value, 0) + 1);
                        }
                    }
                }

                // Find the most frequent value
                String mostFrequentValue = null;
                int maxCount = 0;
                for (Map.Entry<String, Integer> entry : valueCounts.entrySet()) {
                    if (entry.getValue() > maxCount) {
                        mostFrequentValue = entry.getKey();
                        maxCount = entry.getValue();
                    }
                }

                // Pass the most frequent value to the listener
                if (listener != null) {
                    listener.onMostFrequentValue(mostFrequentValue);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
                showToast("Failed to retrieve data: " + databaseError.getMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Define a callback interface
    public interface OnMostFrequentValueListener {
        void onMostFrequentValue(String mostFrequentValue);
    }


}