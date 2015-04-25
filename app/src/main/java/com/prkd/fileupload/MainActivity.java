package com.prkd.fileupload;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements
  LocationListener,
  GoogleApiClient.ConnectionCallbacks,
  GoogleApiClient.OnConnectionFailedListener {
	
	// LogCat tag
	private static final String TAG = MainActivity.class.getSimpleName();
	
 
    // Camera activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
 
    private Uri fileUri; // file url to store image/video
    
    private Button btnCapturePicture;
  private Button btnNearByPicture;
  private String lat;
  private String lon;

  @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Changing action bar background color
        // These two lines are not needed
//        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(getResources().getString(R.color.action_bar))));
 
        btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture);
        btnNearByPicture = (Button) findViewById(R.id.btnNearByPicture);

        /**
         * Capture image button click event
         */
        btnNearByPicture.setOnClickListener(new View.OnClickListener() {
 
            @Override
            public void onClick(View v) {
                // capture picture
                showNearByImage();
            }
        });


    /**
     * Capture image button click event
     */
    btnCapturePicture.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        // capture picture
        captureImage();
      }
    });


      tvLocation = (TextView) findViewById(R.id.tvLocation);

      btnFusedLocation = (Button) findViewById(R.id.btnShowLocation);
      btnFusedLocation.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
          updateUI();
        }
      });

        // Checking camera availability
        if (!isDeviceSupportCamera()) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Your device doesn't support camera",
                    Toast.LENGTH_LONG).show();
            // will close the app if the device does't have camera
            finish();
        }

      createLocationRequest();
      mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build();

    }

  private void showNearByImage() {
    Intent intent = new Intent(this, ShowNearByImages.class);
    intent.putExtra("lat", lat);
    intent.putExtra("lon", lon);

    // start the image capture Intent
    startActivity(intent);
  }

  private static final long INTERVAL = 1000 * 10;
  private static final long FASTEST_INTERVAL = 1000 * 5;

  protected void createLocationRequest() {
    mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(INTERVAL);
    mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  /**
     * Checking device has camera hardware or not
     * */
    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
 
    /**
     * Launching camera app to capture image
     */
    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
 
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
 
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
 
        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    /**
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
 
        // save file url in bundle as it will be null on screen orientation
        // changes
        outState.putParcelable("file_uri", fileUri);
    }
 
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
 
        // get the file url
        fileUri = savedInstanceState.getParcelable("file_uri");
    }
 
    
 
    /**
     * Receiving activity result method will be called after closing the camera
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                
            	// successfully captured the image
                // launching upload activity
            	launchUploadActivity(true);
            	
            	
            } else if (resultCode == RESULT_CANCELED) {
                
            	// user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        
        }
    }
    
    private void launchUploadActivity(boolean isImage){
    	Intent i = new Intent(MainActivity.this, UploadActivity.class);
        i.putExtra("filePath", fileUri.getPath());
        i.putExtra("isImage", isImage);
        i.putExtra("lat", lat);
        i.putExtra("lon", lon);
        startActivity(i);
    }

    /**
     * ------------ Helper Methods ----------------------
     * */

    /**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Config.IMAGE_DIRECTORY_NAME);
 
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create "
                        + Config.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }
 
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }
 
        return mediaFile;
    }


  GoogleApiClient mGoogleApiClient;
  LocationRequest mLocationRequest;
  Location mCurrentLocation;
  String mLastUpdateTime;
  TextView tvLocation;
  Button btnFusedLocation;


  @Override
  public void onConnected(Bundle bundle) {
    Log.d(TAG, "onConnected - isConnected ...............: " + mGoogleApiClient.isConnected());
    startLocationUpdates();
  }

  protected void startLocationUpdates() {
    PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
      mGoogleApiClient, mLocationRequest, this);
    Log.d(TAG, "Location update started ..............: ");
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onLocationChanged(Location location) {
    Log.d(TAG, "Firing onLocationChanged..............................................");
    mCurrentLocation = location;
    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
    updateUI();
  }

  private void updateUI() {
    Log.d(TAG, "UI update initiated .............");
    if (null != mCurrentLocation) {
      lat = String.valueOf(mCurrentLocation.getLatitude());
      lon = String.valueOf(mCurrentLocation.getLongitude());
      tvLocation.setText("At Time: " + mLastUpdateTime + "\n" +
        "Latitude: " + lat + "\n" +
        "Longitude: " + lon + "\n" +
        "Accuracy: " + mCurrentLocation.getAccuracy() + "\n" +
        "Provider: " + mCurrentLocation.getProvider());
    } else {
      Log.d(TAG, "location is null ...............");
    }
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.d(TAG, "Connection failed: " + connectionResult.toString());
  }

  @Override
  public void onResume() {
    super.onStart();
    Log.d(TAG, "onStart fired ..............");
    mGoogleApiClient.connect();
  }

  @Override
  public void onPause() {
    super.onStop();
    Log.d(TAG, "onStop fired ..............");
    mGoogleApiClient.disconnect();
    Log.d(TAG, "isConnected ...............: " + mGoogleApiClient.isConnected());
  }
}