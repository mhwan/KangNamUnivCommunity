package com.yscn.knucommunity.CustomView;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.yscn.knucommunity.R;

/**
 * Created by GwonHyeok on 14. 10. 22..
 */
public class BaseDoubleKillActivity extends ActionBarActivity {
    private Handler backKeyHandler = new Handler();
    private int backKeyCount = 2;
    private Toast toast;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    @Override
    public void onBackPressed() {
        int resetBackKeyCountTimemills = 1300;
        if (--backKeyCount == 0) {
            super.onBackPressed();
        } else {
            toast.setText(getString(R.string.double_back_kill_app));
            toast.show();
        }
        backKeyHandler.postDelayed(new backPreesedRunnable(), resetBackKeyCountTimemills);
    }

    private class backPreesedRunnable implements Runnable {

        @Override
        public void run() {
            backKeyCount = 2;
        }
    }
}
