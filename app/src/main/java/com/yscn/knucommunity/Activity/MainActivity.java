package com.yscn.knucommunity.Activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.yscn.knucommunity.CustomView.BaseDoubleKillActivity;
import com.yscn.knucommunity.R;

/**
 * Created by GwonHyeok on 14. 10. 22..
 */
public class MainActivity extends BaseDoubleKillActivity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        viewInit();
        findViewById(R.id.main_studentground).setOnClickListener(this);
        findViewById(R.id.main_notice).setOnClickListener(this);
        findViewById(R.id.main_market).setOnClickListener(this);
        findViewById(R.id.main_community).setOnClickListener(this);
        findViewById(R.id.main_link).setOnClickListener(this);
        findViewById(R.id.main_setting).setOnClickListener(this);
//        setContent(ACTIVITY.Communitty);

    }

    private void viewInit() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(0xFFEBECF0);
        }
        getSupportActionBar().hide();
    }

    public void setContent(ACTIVITY activity) {
        if (activity == ACTIVITY.STUDENTINFO) {
            setContentView(R.layout.activity_studentinfo);
            getSupportActionBar().hide();
        } else if (activity == ACTIVITY.LINK) {
            setContentView(R.layout.activity_link);
            getSupportActionBar().hide();
        } else if (activity == ACTIVITY.RESTRAUNT) {
            setContentView(R.layout.activity_restraunt);
            getSupportActionBar().hide();
        } else if (activity == ACTIVITY.STUDENTGROUND) {
            startActivity(new Intent(this, StudentGroundActivity.class));
        } else if (activity == ACTIVITY.MARKET) {
            startActivity(new Intent(this, MarketMainActivity.class));
        } else if (activity == ACTIVITY.Communitty) {
            startActivity(new Intent(this, CommunittyActivity.class));
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.main_studentground) {
            startActivity(new Intent(this, StudentGroundActivity.class));
        } else if (id == R.id.main_notice) {
            startActivity(new Intent(this, NoticeActivity.class));
        } else if (id == R.id.main_market) {
            startActivity(new Intent(this, MarketMainActivity.class));
        } else if (id == R.id.main_community) {
            startActivity(new Intent(this, CommunittyActivity.class));
        } else if (id == R.id.main_link) {
            startActivity(new Intent(this, LinkActivity.class));
        } else if (id == R.id.main_setting) {

        }
    }

    public enum ACTIVITY {STUDENTINFO, LINK, RESTRAUNT, STUDENTGROUND, MARKET, Communitty}
}