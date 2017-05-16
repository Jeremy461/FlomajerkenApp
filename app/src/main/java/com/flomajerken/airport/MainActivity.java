package com.flomajerken.airport;

import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CODE = 1;
    private String[] permissions = {android.Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_camera = (Button) findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_camera:
                Intent cameraIntent = new Intent(this, CameraActivity.class);
                startActivity(cameraIntent);
        }
    }
}
