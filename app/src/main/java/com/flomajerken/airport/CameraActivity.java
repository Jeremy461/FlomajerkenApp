package com.flomajerken.airport;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CameraActivity extends AppCompatActivity {

    private static final String LOG_TAG = "flomajerken";

    private Camera mCamera;
    private CameraPreview mPreview;

    //Sensor shit
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor gravity;
    private SensorEventListener sensorListener;

    private TextView tv_gyro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        tv_gyro = (TextView) findViewById(R.id.tv_gyro);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        if(gyroscope == null || gravity == null){
            Toast.makeText(this, "Your device doesn't have the required sensors for this app.", Toast.LENGTH_SHORT).show();
            finish();
        }

        sensorListener = new SensorEventListener() {

            //Opvangen van sensor data
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {

                if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    if(sensorEvent.values[1] > 0.5f){
                        tv_gyro.setText("Down");
                    }
                    else if(sensorEvent.values[1] < -0.5f){
                        tv_gyro.setText("Up");
                    }
                }

                if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    Log.d(LOG_TAG, Float.toString(sensorEvent.values[1]));
                    if(sensorEvent.values[1] > 0f){
                        rotateLine(-sensorEvent.values[1] / 1.5f);
                    }
                    else if(sensorEvent.values[1] < 0f){
                        rotateLine(-sensorEvent.values[1] / 1.5f);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        if(checkCameraHardware(this)){

            // Create an instance of Camera
            mCamera = getCameraInstance();

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
    }



    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** Rotate horizontal line */
    public void rotateLine(Float angle){
        View imageView = findViewById(R.id.line);
        imageView.setRotation(angle * (9f));
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.d(LOG_TAG, "Camera is not available");
//            Toast.makeText(CameraActivity.this, "Camera is not available", Toast.LENGTH_SHORT).show();
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorListener, gravity, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }
}
