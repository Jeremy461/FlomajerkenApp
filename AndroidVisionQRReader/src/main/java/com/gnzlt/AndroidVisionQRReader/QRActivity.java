package com.gnzlt.AndroidVisionQRReader;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gnzlt.AndroidVisionQRReader.camera.CameraSourcePreview;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QRActivity extends AppCompatActivity {

    private static final String LOG_TAG = "flomajerken";
    public static final String EXTRA_QR_RESULT = "EXTRA_QR_RESULT";
    private static final String TAG = "QRActivity";
    private static final int PERMISSIONS_REQUEST = 100;

    private BarcodeDetector mBarcodeDetector;
    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private SensorManager sensorManager;
    private Sensor gravity;
    private Sensor rotation;
    private SensorEventListener sensorListener;

    private boolean verticalDirection;
    private boolean gravityMiddle = false;
    private float altitude = 0;
    private float radarRotation = 0;
    private int mAzimuth = 0;
    private String name;

    private final String url = "http://145.24.222.211:8000/flomajerkenAPI/users/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        name = getIntent().getStringExtra("name");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if (rotation == null || gravity == null) {
            Toast.makeText(this, "Your device doesn't have the required sensors for this app.", Toast.LENGTH_SHORT).show();
            finish();
        }

        mPreview = (CameraSourcePreview) findViewById(R.id.cameraSourcePreview);

        if (isPermissionGranted()) {
            setupBarcodeDetector();
            setupCameraSource();
        } else {
            requestPermission();
        }


        sensorListener = new SensorEventListener() {

            float[] orientation = new float[3];
            float[] rMat = new float[9];

            //Opvangen van sensor data
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                ImageView line = (ImageView) findViewById(R.id.line);

                if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    if (sensorEvent.values[2] > 9.5f) {
                        gravityMiddle = true;
                    } else {
                        gravityMiddle = false;
                    }

                    altitude = altitude - (0.2f * (sensorEvent.values[2] - 2));
//                    Log.d(LOG_TAG, sensorEvent.values[2] + "");

                    if (altitude > 55) {
                        altitude = 55;
                    } else if (altitude < -55) {
                        altitude = -55;
                    }

                    if (altitude > 50) {
                        ImageView img = (ImageView) findViewById(R.id.iv_cockpit);
                        img.setImageResource(R.drawable.cockpit_bg_frozen);
                        line.setVisibility(View.INVISIBLE);
                        View crateCarry = findViewById(R.id.crateCarry);
                        crateCarry.setVisibility(View.INVISIBLE);
                        finish();

                    } else if (altitude < -50) {
                        ImageView img = (ImageView) findViewById(R.id.iv_cockpit);
                        img.setImageResource(R.drawable.cockpit_bg_cracks);
                        line.setVisibility(View.INVISIBLE);
                        View crateCarry = findViewById(R.id.crateCarry);
                        crateCarry.setVisibility(View.INVISIBLE);
                        finish();
                    } else {
                        ImageView img = (ImageView) findViewById(R.id.iv_cockpit);
                        img.setImageResource(R.drawable.cockpit_2);
                        line.setVisibility(View.VISIBLE);
                    }

                    View imageView = findViewById(R.id.heightIndicator);
                    imageView.setTranslationY(-altitude);

                    if (sensorEvent.values[1] > 0f) {
                        rotate((-sensorEvent.values[1] / 1.5f) * 9f, findViewById(R.id.line));
                    } else if (sensorEvent.values[1] < 0f) {
                        rotate((-sensorEvent.values[1] / 1.5f) * 9f, findViewById(R.id.line));
                    }
                }

                if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    // calculate th rotation matrix
                    SensorManager.getRotationMatrixFromVector(rMat, sensorEvent.values);
                    // get the azimuth value (orientation[0]) in degree
                    mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;

                    rotate((float) -mAzimuth, findViewById(R.id.iv_cockpit_radar_marker));

                    View crate = findViewById(R.id.crate);

                    if (mAzimuth >= 0 && mAzimuth <= 180) {
                        crate.setTranslationX((-mAzimuth) * 15);
                    } else if (mAzimuth > 180 && mAzimuth < 360) {
                        crate.setTranslationX((mAzimuth - 360) * -15);
                    }

                    if (mAzimuth >= 0 && mAzimuth <= 25) {
                        if (crate.getScaleX() < 2) {
                            crate.setScaleX(crate.getScaleX() + 0.001f);
                            crate.setScaleY(crate.getScaleY() + 0.001f);
                        }
                    } else if (mAzimuth >= 335) {
                        if (crate.getScaleX() < 2) {
                            crate.setScaleX(crate.getScaleX() + 0.001f);
                            crate.setScaleY(crate.getScaleY() + 0.001f);
                        }
                    }

//                    Log.d(LOG_TAG, crate.getTranslationX()+ "");
//                    Log.d(LOG_TAG, mAzimuth +"");
//                    Log.d(LOG_TAG, spawnLocation +"");

                }


                rotate(-radarRotation, findViewById(R.id.iv_cockpit_radar_line));
                rotate((float) -mAzimuth, findViewById(R.id.iv_cockpit_radar));
                radarRotation++;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPermissionGranted())
            startCameraSource();
        sensorManager.registerListener(sensorListener, gravity, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorListener, rotation, SensorManager.SENSOR_DELAY_GAME);
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    recreate();
                }
            } else {
                Toast.makeText(this, "This application needs Camera permission to read QR codes", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Rotate horizontal line
     */
    public void rotate(Float angle, View view) {
        view.setRotation(angle);
    }

    private void setupBarcodeDetector() {
        mBarcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        mBarcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                RequestQueue queue = Volley.newRequestQueue(QRActivity.this);
                if (barcodes.size() != 0) {
                    String data = barcodes.valueAt(0).displayValue;

                    Log.d(TAG, "Barcode detected: " + data);
                    final JSONObject body = new JSONObject();
                    try {
                        body.put("name", name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, body,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    // response
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    // error
                                }
                            }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("name", name);

                            return params;
                        }

                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("Accept", "application/json");
                            params.put("Content-Type", "application/json");

                            return params;
                        }
                    };
                    queue.add(postRequest);
//                    VolleySingleton.getInstance(QRActivity.this).addToRequestQueue(jsonObjectRequest);

                    returnData(data);
                }
            }
        });

        if (!mBarcodeDetector.isOperational())
            Log.w(TAG, "Detector dependencies are not yet available.");
    }

    private void setupCameraSource() {
        mCameraSource = new CameraSource.Builder(getApplicationContext(), mBarcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15.0f)
                .setRequestedPreviewSize(1600, 1024)
                .setAutoFocusEnabled(true)
                .build();
    }

    private void startCameraSource() {
        Log.d(TAG, "Camera Source started");
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void returnData(String data) {
        if (data != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_QR_RESULT, data);
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
        sensorManager.unregisterListener(sensorListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }
}
