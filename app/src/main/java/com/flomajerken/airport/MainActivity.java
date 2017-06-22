package com.flomajerken.airport;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CODE = 1;
    private String[] permissions = {android.Manifest.permission.CAMERA};
    private String name;
    private final String url = "http://145.24.222.211:8000/flomajerkenAPI/users/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton btn_camera = (ImageButton) findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(this);
}

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_camera:
                EditText mEdit = (EditText) findViewById(R.id.name);
                name = mEdit.getText().toString();

                if(name.equals("Name") || name.equals("")){
                    Toast.makeText(MainActivity.this, "Name is required", Toast.LENGTH_SHORT).show();
                } else {
                    final JSONObject body = new JSONObject();
                    try {
                        body.put("name", "Henk");
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
                            params.put("name", "Henk");

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

                    Intent cameraIntent = new Intent(this, CameraActivity.class);
                    cameraIntent.putExtra("name", this.name);
                    startActivity(cameraIntent);
                }


        }
    }


}
