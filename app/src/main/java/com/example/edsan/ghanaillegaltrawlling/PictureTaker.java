package com.example.edsan.ghanaillegaltrawlling;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import java.io.File;
import java.io.IOException;


public class PictureTaker extends Activity {

    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private EditText descriptionEditText;
    private Button takePhotoButton;
    private Button sendReportButton;
    private ImageView trawlingPhoto;
    private Bitmap photograph;
    private LinearLayout pictureLinearLayout;
    private File f = null;
    private static int CAMERA_REQUEST = 1000;
    private LocationManager locationManager = null;
    private double latitude;
    private double longitude;
    private Location location = null;
    private static String PHOTOGRAPH = "PHOTOGRAPH";
    private static String LATITUDE = "LATITUDE";
    private static String LONGITUDE = "LONGITUDE";
    private static String DESCRIPTION = "DESCRIPTION";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_taker);
        latitudeEditText = (EditText) findViewById(R.id.latitudeEditText);
        longitudeEditText = (EditText) findViewById(R.id.longitudeEditText);
        descriptionEditText = (EditText)findViewById(R.id.descriptionEditText);
        takePhotoButton = (Button) findViewById(R.id.takePhotoButton);
        sendReportButton = (Button) findViewById(R.id.sendReportButton);
        trawlingPhoto = (ImageView) findViewById(R.id.trawllingImageView);
        pictureLinearLayout = (LinearLayout) findViewById(R.id.pictureLinearLayout);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(Context.LOCATION_SERVICE)){
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }

        takePhotoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        sendReportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIncidentReport();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.picture_taker, menu);
        return true;
    }

    public void takePhoto(){
        Intent pictureIntent = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
        if(pictureIntent.resolveActivity(getPackageManager())!= null){
            startActivityForResult(pictureIntent, CAMERA_REQUEST);//starting camera activity that will return a result after it starts
        }
    }

    public void sendIncidentReport(){
        Thread t = new Thread(){
            public void run() {
                HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
                try {

                    HttpPost post = new HttpPost("http://192.168.1.111:8000/incident_report/report/");

                    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                    entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    if (f != null){
                        entityBuilder.addBinaryBody(PHOTOGRAPH, f);
                    }

                    entityBuilder.addTextBody(LONGITUDE, "" + longitude);
                    entityBuilder.addTextBody(LATITUDE, "" + latitude);
                    entityBuilder.addTextBody(DESCRIPTION, descriptionEditText.getText().toString());
                    HttpEntity entity = entityBuilder.build();
                    post.setEntity(entity);

                    client.execute(post);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        t.start();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST){
            if (resultCode == RESULT_OK){
                try {
                    photograph = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                    Cursor cursor = null;
                    try {
                        String[] proj = { MediaStore.Images.Media.DATA };
                        cursor = this.getContentResolver().query(data.getData(),  proj, null, null, null);
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        cursor.moveToFirst();
                        f = new File(cursor.getString(column_index));
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    Bitmap pictureForImageView = Bitmap.createScaledBitmap(photograph, pictureLinearLayout.getWidth(), pictureLinearLayout.getHeight(), false);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(pictureForImageView, 0,0, pictureForImageView.getWidth(), pictureForImageView.getHeight(), matrix, true);
                    trawlingPhoto.setImageBitmap(rotatedBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                latitudeEditText.setText(""+latitude);
                longitudeEditText.setText(""+longitude);
            }
        }
    }
}
