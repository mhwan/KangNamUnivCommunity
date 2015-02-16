package com.yscn.knucommunity.Activity;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;
import com.yscn.knucommunity.R;
import com.yscn.knucommunity.Ui.StudentNotificationPagerAdapter;

/**
 * Created by GwonHyeok on 15. 2. 3..
 */
public class StudentNotificationActivity extends ActionBarActivity {
    private PagerSlidingTabStrip mSlidingTabStrip;
    private ViewPager mViewPager;
    private StudentNotificationPagerAdapter mNotificationPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentinfonotification);
        toolbarInit();
        viewInit();
    }

    private void viewInit() {
        mSlidingTabStrip = (PagerSlidingTabStrip) findViewById(R.id.studentnotify_pagertabstrip);
        mViewPager = (ViewPager) findViewById(R.id.studentnotify_viewpager);
        mNotificationPagerAdapter = new StudentNotificationPagerAdapter(this);
        mNotificationPagerAdapter.setPageTitle(getResources().getStringArray(R.array.studentinfo_tab_title));
        mViewPager.setAdapter(mNotificationPagerAdapter);
        mSlidingTabStrip.setViewPager(mViewPager);
        mSlidingTabStrip.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mNotificationPagerAdapter.resetRefresh();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void toolbarInit() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle(R.string.text_studentinfo_notify_title);
        toolbar.setTitleTextColor(0xFFFFFFFF);
        toolbar.setNavigationIcon(R.drawable.ic_nav_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StudentNotificationActivity.this.onBackPressed();
            }
        });
    }
}
