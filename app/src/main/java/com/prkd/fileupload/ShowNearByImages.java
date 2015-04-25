package com.prkd.fileupload;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class ShowNearByImages extends Activity{
  private ProgressBar progressBar;
  long totalSize = 0;
  private String lat;
  private String lon;
  TextView imagesLocation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_near_by_images);
    imagesLocation = (TextView) findViewById(R.id.imagesLocation);
    progressBar = (ProgressBar) findViewById(R.id.progressBar);

    // Receiving the data from previous activity
    Intent i = getIntent();

    // boolean flag to identify the media type, image or video
//    boolean isImage = i.getBooleanExtra("isImage", true);
    lat = i.getStringExtra("lat");
    lon = i.getStringExtra("lon");
    new RetrieveImageLocationFromServer().execute();
  }


  private class RetrieveImageLocationFromServer extends AsyncTask<Void, Integer, String> {
    @Override
    protected void onPreExecute() {
      // setting progress bar to zero
      progressBar.setProgress(0);
      super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
      // Making progress bar visible
      progressBar.setVisibility(View.VISIBLE);

      // updating progress bar value
      progressBar.setProgress(progress[0]);
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
              publishProgress((int) ((num / (float) totalSize) * 100));
            }
          });

        // Extra parameters if you want to pass to server
        entity.addPart("lat",new StringBody(lat));
        entity.addPart("lon", new StringBody(lon));

        totalSize = entity.getContentLength();
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
    imagesLocation.setText(result);
  }
}
