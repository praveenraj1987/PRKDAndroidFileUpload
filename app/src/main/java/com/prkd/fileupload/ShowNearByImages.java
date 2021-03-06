package com.prkd.fileupload;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

public class ShowNearByImages  extends FragmentActivity {
  private ProgressBar progressBar;
  long totalSize = 0;
  private String lat="";
  private String lon="";
  TextView imagesLocation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Receiving the data from previous activity
    Intent i = getIntent();

    // boolean flag to identify the media type, image or video
//    boolean isImage = i.getBooleanExtra("isImage", true);
    lat = i.getStringExtra("lat");
    lon = i.getStringExtra("lon");

    ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(getApplicationContext());
    config.threadPriority(Thread.NORM_PRIORITY - 2);
    config.denyCacheImageMultipleSizesInMemory();
    config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
    config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
    config.tasksProcessingOrder(QueueProcessingType.LIFO);
    config.writeDebugLogs(); // Remove for release app

    // Initialize ImageLoader with configuration.
    ImageLoader.getInstance().init(config.build());
    Fragment fr;
    String tag;
    int titleRes;
    tag = ImagePagerFragment.class.getSimpleName();
    fr = getSupportFragmentManager().findFragmentByTag(tag);
    if (fr == null) {
      fr = new ImagePagerFragment();
      fr.setArguments(getIntent().getExtras());
    }
    titleRes = R.string.ac_name_image_pager;

    setTitle(titleRes);
    getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fr, tag).commit();
  }
}
