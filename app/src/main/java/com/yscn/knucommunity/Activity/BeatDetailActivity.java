package com.yscn.knucommunity.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.yscn.knucommunity.R;
import com.yscn.knucommunity.Util.ApplicationUtil;
import com.yscn.knucommunity.Util.ImageLoaderUtil;
import com.yscn.knucommunity.Util.NetworkUtil;
import com.yscn.knucommunity.Util.UrlList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by GwonHyeok on 15. 2. 19..
 */
public class BeatDetailActivity extends ActionBarActivity {
    private int mBeatIndex;
    private String mContentId;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_beat_detail);
        toolbarInit();
        setBeatIntentData();
        setBeatContentData();
        ApplicationUtil.getInstance().setTypeFace(findViewById(R.id.beat_detail_root));
    }

    private void toolbarInit() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mToolbar.setNavigationIcon(R.drawable.ic_cancel);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setBeatContentData() {
        new AsyncTask<Void, Void, JSONObject>() {
            private ProgressBar mProgressbar;

            @Override
            protected void onPreExecute() {
                mProgressbar = (ProgressBar) findViewById(R.id.beat_detail_progressbar);
                mProgressbar.setVisibility(View.VISIBLE);
            }

            @Override
            protected JSONObject doInBackground(Void... params) {
                HashMap<String, String> parameter = new HashMap<>();
                parameter.put("beatid", String.valueOf(mBeatIndex + 1));
                parameter.put("contentid", mContentId);
                try {
                    return NetworkUtil.getInstance().getBeatDetail(parameter);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                mProgressbar.setVisibility(View.GONE);
                invalidateOptionsMenu();
                Log.d(getClass().getSimpleName(), jsonObject.toJSONString());
                JSONArray attatchmentArray = (JSONArray) jsonObject.get("attachment");

                String[] attatchmenturls = new String[attatchmentArray.size()];
                for (int i = 0; i < attatchmentArray.size(); i++) {
                    JSONObject attatchmentObject = (JSONObject) attatchmentArray.get(i);
                    attatchmenturls[i] = attatchmentObject.get("filename").toString();
                }
                addPhotoView(attatchmenturls);

                try {
                    ((TextView) findViewById(R.id.beat_detail_title)).setText(jsonObject.get("title").toString());
                    ((TextView) findViewById(R.id.beat_detail_content)).setText(jsonObject.get("content").toString());
                } catch (Exception e) {

                }

            }
        }.execute();
    }

    private void addPhotoView(String[] urls) {
        ImageLoaderUtil.getInstance().initImageLoader();
        LinearLayout imageGroup = (LinearLayout) findViewById(R.id.beat_detail_scrollview_imagegroup);

        for (String url : urls) {
            View view = getLayoutInflater().inflate(R.layout.ui_board_image_card, imageGroup, false);
            final ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
            final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressbar);
            ImageLoader.getInstance().displayImage(UrlList.MAIN_URL + url, imageView,
                    ImageLoaderUtil.getInstance().getNoCacheImageOptions(), new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            progressBar.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
            imageGroup.addView(view);
        }
    }

    private void setBeatIntentData() {
        Intent intent = getIntent();
        mBeatIndex = intent.getIntExtra("beatindex", -1);
        mContentId = intent.getStringExtra("contentid");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mBeatIndex == 3) {
            getMenuInflater().inflate(R.menu.board_detail_menu, menu);
            menu.getItem(1).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_trash) {

        }
        return super.onOptionsItemSelected(item);
    }
}