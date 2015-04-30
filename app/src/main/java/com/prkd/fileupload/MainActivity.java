package com.prkd.fileupload;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
    
    private ImageButton btnCapturePicture;
  private ImageButton btnNearByPicture;
  private String lat;
  private String lon;

  @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Changing action bar background color
        // These two lines are not needed
//        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(getResources().getString(R.color.action_bar))));

      AdView mAdView = (AdView) findViewById(R.id.adView);
      AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("07CD19292E4218769CA8F1A59E845F29").build();
      mAdView.loadAd(adRequest);
        btnCapturePicture = (ImageButton) findViewById(R.id.btnCapturePicture);
        btnNearByPicture = (ImageButton) findViewById(R.id.btnNearByPicture);

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

  public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
    long diffInMillies = date2.getTime() - date1.getTime();
    return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
  }

  private void showNearByImage() {
//    Intent intent = new Intent(this, ShowNearByImages.class);
//    intent.putExtra("lat", lat);
//    intent.putExtra("lon", lon);

    if((!(lat == null || lon == null)) && (mLastUpdateTime == null || getDateDiff(mLastUpdateTime, new Date(), TimeUnit.SECONDS) < 60 )) {
      new RetrieveImageLocationFromServer().execute();
    }
    else{
      new AlertDialog.Builder(this)
        .setTitle("Free Parking Spot")
        .setMessage("GPS not wokring!! Please turn on and Try Again!")
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // continue with delete
          }
        })
        .setIcon(android.R.drawable.ic_dialog_alert)
        .show();
    }
    // start the image capture Intent
//    startActivity(intent);
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
        i.putExtra("mLastUpdateTime", mLastUpdateTime);

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
  Date mLastUpdateTime;
//  TextView tvLocation;
//  Button btnFusedLocation;


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
    mLastUpdateTime = new Date();
    updateUI();
  }

  private void updateUI() {
    Log.d(TAG, "UI update initiated .............");
    if (null != mCurrentLocation) {
      lat = String.valueOf(mCurrentLocation.getLatitude());
      lon = String.valueOf(mCurrentLocation.getLongitude());
//      tvLocation.setText("At Time: " + mLastUpdateTime + "\n" +
//        "Latitude: " + lat + "\n" +
//        "Longitude: " + lon + "\n" +
//        "Accuracy: " + mCurrentLocation.getAccuracy() + "\n" +
//        "Provider: " + mCurrentLocation.getProvider());
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


  private class RetrieveImageLocationFromServer extends AsyncTask<Void, Integer, String> {
    @Override
    protected void onPreExecute() {
      // setting progress bar to zero
      super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
    }

    @Override
    protected String doInBackground(Void... params) {
      return uploadFile();
    }

    @SuppressWarnings("deprecation")
    private String uploadFile() {
      String responseString = null;

      HttpClient httpclient = new DefaultHttpClient();
      HttpPost httppost = new HttpPost(Config.GET_IMAGE_URL);

      try {
        AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
          new AndroidMultiPartEntity.ProgressListener() {

            @Override
            public void transferred(long num) {
            }
          });

        // Extra parameters if you want to pass to server
        entity.addPart("lat",new StringBody(lat));
        entity.addPart("lon", new StringBody(lon));

//        totalSize = entity.getContentLength();
        httppost.setEntity(entity);

        // Making server call
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity r_entity = response.getEntity();

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
          // Server response
          responseString = EntityUtils.toString(r_entity);
        } else {
          responseString = "Error occurred! Http Status Code: "
            + statusCode;
        }

      } catch (ClientProtocolException e) {
        responseString = e.toString();
      } catch (IOException e) {
        responseString = e.toString();
      }

      return responseString;

    }

    @Override
    protected void onPostExecute(String result) {
      processResult(result);

      super.onPostExecute(result);
    }

  }

  private void processResult(String result) {
    String[] urlList = new String[0];
    try {
      JSONObject json;
      json = new JSONObject(result);
      JSONArray imageList = json.getJSONArray("ImageList");
      urlList = new String[imageList.length()];
      for(int i = 0 ; i < imageList.length(); i++){
        urlList[i] = ((JSONObject )imageList.get(i)).getString("image");
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    if(urlList.length > 0) {

      Intent intent = new Intent(this, ShowNearByImages.class);
      intent.putExtra("urls", urlList);
      startActivity(intent);
    }
    else{
      new AlertDialog.Builder(this)
        .setTitle("Free Parking Spot")
        .setMessage("No Free parking spot near by 1000Mts.")
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // continue with delete
          }
        })
        .setIcon(android.R.drawable.ic_dialog_alert)
        .show();
    }
  }

}