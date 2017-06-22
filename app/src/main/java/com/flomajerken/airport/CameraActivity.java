package com.flomajerken.airport;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.gnzlt.AndroidVisionQRReader.QRActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CameraActivity extends AppCompatActivity {

    private static final String LOG_TAG = "flomajerken";

    private Camera mCamera;
    private CameraPreview mPreview;

    //Sensor shit
    private SensorManager sensorManager;
    private Sensor gravity;
    private Sensor rotation;
    private SensorEventListener sensorListener;

    private boolean verticalDirection;
    private boolean gravityMiddle = false;
    private float altitude = 0;
    private float radarRotation = 0;
    private float crateSpawn;
    private boolean crateSpawned = false;
    private float spawnLocation = (int) Math.ceil(Math.random() * 360);

    private int mAzimuth = 0;
    private ImageView crate;

    public static final int QR_REQUEST = 111;

    private final String url_now = "http://api.openweathermap.org/data/2.5/weather?q=Rotterdam&units=imperial&APPID=dfc773520361124b21a2723ee480b329";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if(rotation == null || gravity == null){
            Toast.makeText(this, "Your device doesn't have the required sensors for this app.", Toast.LENGTH_SHORT).show();
            finish();
        }

        sensorListener = new SensorEventListener() {

            float[] orientation = new float[3];
            float[] rMat = new float[9];

            //Opvangen van sensor data
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                ImageView line = (ImageView) findViewById(R.id.line);

                if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    if(sensorEvent.values[2] > 9.5f){
                        gravityMiddle = true;
                    } else {
                        gravityMiddle = false;
                    }

                    altitude = altitude - (0.2f * (sensorEvent.values[2]-2));
//                    Log.d(LOG_TAG, sensorEvent.values[2] + "");

                    if(altitude > 55){
                        altitude = 55;
                    } else if(altitude < -55){
                        altitude = -55;
                    }

                    if(altitude > 50){
                        ImageView img = (ImageView) findViewById(R.id.iv_cockpit);
                        img.setImageResource(R.drawable.cockpit_bg_frozen);
                        line.setVisibility(View.INVISIBLE);
                        View crateCarry = findViewById(R.id.crateCarry);
                        crateCarry.setVisibility(View.INVISIBLE);

                    } else if(altitude < -50){
                        ImageView img = (ImageView) findViewById(R.id.iv_cockpit);
                        img.setImageResource(R.drawable.cockpit_bg_cracks);
                        line.setVisibility(View.INVISIBLE);
                        View crateCarry = findViewById(R.id.crateCarry);
                        crateCarry.setVisibility(View.INVISIBLE);
                    } else {
                        ImageView img = (ImageView) findViewById(R.id.iv_cockpit);
                        img.setImageResource(R.drawable.cockpit_2);
                        line.setVisibility(View.VISIBLE);
                    }

                    View imageView = findViewById(R.id.heightIndicator);
                    imageView.setTranslationY(-altitude);

                    if(sensorEvent.values[1] > 0f){
                        rotate((-sensorEvent.values[1] / 1.5f) * 9f, findViewById(R.id.line));
                    }
                    else if(sensorEvent.values[1] < 0f){
                        rotate((-sensorEvent.values[1] / 1.5f) * 9f, findViewById(R.id.line));
                    }
                }

                if( sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ){
                    // calculate th rotation matrix
                    SensorManager.getRotationMatrixFromVector( rMat, sensorEvent.values );
                    // get the azimuth value (orientation[0]) in degree
                    mAzimuth = (int) ( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;

                    rotate( (float) -mAzimuth, findViewById(R.id.iv_cockpit_radar_marker));

                    View crate = findViewById(R.id.crate);

                    if(mAzimuth >= 0 && mAzimuth <= 180) {
                        crate.setTranslationX((-mAzimuth) * 15);
                    } else if(mAzimuth > 180 && mAzimuth < 360) {
                        crate.setTranslationX((mAzimuth -360) * -15);
                    }

                    if(mAzimuth >= 0 && mAzimuth <=25 && crateSpawned){
                        if(crate.getScaleX() < 2){
                            crate.setScaleX(crate.getScaleX()+0.001f);
                            crate.setScaleY(crate.getScaleY()+0.001f);
                        }
                    }
                    else if(mAzimuth >= 335 && crateSpawned){
                        if(crate.getScaleX() < 2){
                            crate.setScaleX(crate.getScaleX()+0.001f);
                            crate.setScaleY(crate.getScaleY()+0.001f);
                        }
                    }

//                    Log.d(LOG_TAG, crate.getTranslationX()+ "");
//                    Log.d(LOG_TAG, mAzimuth +"");
//                    Log.d(LOG_TAG, spawnLocation +"");

                }

                if(!crateSpawned){
                    crateSpawn = (int) Math.ceil(Math.random() * 1000);
                    if(crateSpawn == 1){
                        crateSpawned = true;
                        crate.setScaleX(0.1f);
                        crate.setScaleY(0.1f);
                        crate.setVisibility(View.VISIBLE);
                        View radarMarker = findViewById(R.id.iv_cockpit_radar_marker);
                        radarMarker.setVisibility(View.VISIBLE);

                        View crateCarry = findViewById(R.id.crateCarry);
                        crateCarry.setVisibility(View.INVISIBLE);
                    }
                }


                crate = (ImageView)findViewById(R.id.crate);


                crate.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        crateSpawned = false;
                        crate.setVisibility(View.INVISIBLE);
                        View radarMarker = findViewById(R.id.iv_cockpit_radar_marker);
                        radarMarker.setVisibility(View.INVISIBLE);

                        View crateCarry = findViewById(R.id.crateCarry);
                        crateCarry.setVisibility(View.INVISIBLE);

                        requestQRCodeScan(crate);
                    }
                });


                rotate(-radarRotation, findViewById(R.id.iv_cockpit_radar_line));
                rotate( (float) -mAzimuth, findViewById(R.id.iv_cockpit_radar));
                radarRotation++;
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


    public void requestQRCodeScan(View v) {
        Intent qrScanIntent = new Intent(this, QRActivity.class);
        String name = getIntent().getStringExtra("name");
        qrScanIntent.putExtra("name", name);
        startActivityForResult(qrScanIntent, QR_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == QR_REQUEST) {
            if (resultCode == RESULT_OK) {
                String qrData = data.getStringExtra(QRActivity.EXTRA_QR_RESULT);
                // do something with the QR data String
                Log.d(LOG_TAG, qrData);
            } else {
                //Error handler
            }
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
    public void rotate(Float angle, View view){
        view.setRotation(angle);
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
        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        super.onResume();
        sensorManager.registerListener(sensorListener, gravity, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorListener, rotation, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }
}
