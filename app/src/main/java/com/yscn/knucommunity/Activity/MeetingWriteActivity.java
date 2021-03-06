package com.yscn.knucommunity.Activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.yscn.knucommunity.CustomView.ClearProgressDialog;
import com.yscn.knucommunity.R;
import com.yscn.knucommunity.Ui.AlertToast;
import com.yscn.knucommunity.Util.ApplicationUtil;
import com.yscn.knucommunity.Util.NetworkUtil;

import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 * Created by GwonHyeok on 14. 11. 5..
 */
public class MeetingWriteActivity extends ActionBarActivity implements View.OnClickListener {
    private boolean isMale = true;
    private TextView genderTextView;
    private EditText contentView, schoolView, majorView, countView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_meeting_write);
        actionBarInit();
        viewInit();
        ApplicationUtil.getInstance().setTypeFace(getWindow().getDecorView());
    }

    private void viewInit() {
        genderTextView = (TextView) findViewById(R.id.meeting_write_gender_textview);
        contentView = (EditText) findViewById(R.id.meeting_content_edittext);
        schoolView = (EditText) findViewById(R.id.meeting_school_edittext);
        majorView = (EditText) findViewById(R.id.meeting_major_edittext);
        countView = (EditText) findViewById(R.id.meeting_studentcount_edittext);

        findViewById(R.id.meeting_write_gender).setOnClickListener(this);
        findViewById(R.id.button).setOnClickListener(this);

    }

    private void actionBarInit() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationIcon(R.drawable.ic_cancel);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showGenderSelectDialog() {
        final CharSequence[] genderArray = getResources().getStringArray(R.array.gender_list);
        final TextView genderTextView = (TextView) findViewById(R.id.meeting_write_gender_textview);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDialog = builder.setSingleChoiceItems(genderArray, isMale ? 0 : 1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isMale = (which == 0);
            }
        }).setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                genderTextView.setText(isMale ? genderArray[0] : genderArray[1]);
            }
        }).setTitle(R.string.community_gender_dialog_title).show();
        ApplicationUtil.getInstance().setTypeFace(alertDialog.getWindow().getDecorView());
    }

    private boolean checkValidData() {
        if (genderTextView.getText().toString().isEmpty()) {
            AlertToast.warning(getContext(), getString(R.string.warning_meeting_set_gender));
            return false;
        } else if (contentView.getText().toString().isEmpty()) {
            AlertToast.warning(getContext(), getString(R.string.warning_board_write_content));
            return false;
        } else if (schoolView.getText().toString().isEmpty()) {
            AlertToast.warning(getContext(), getString(R.string.warning_meeting_set_schoolname));
            return false;
        } else if (majorView.getText().toString().isEmpty()) {
            AlertToast.warning(getContext(), getString(R.string.warning_meeting_set_majorname));
            return false;
        } else if (countView.getText().toString().isEmpty()) {
            AlertToast.warning(getContext(), getString(R.string.warning_meeting_set_peoplecount));
            return false;
        } else {
            return true;
        }
    }

    private void writeMeetingBoard() {
        new AsyncTask<Void, Void, Boolean>() {
            private ClearProgressDialog clearProgressDialog;

            @Override
            protected void onPreExecute() {
                clearProgressDialog = new ClearProgressDialog(getContext());
                clearProgressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;
                EditText contentView = (EditText) findViewById(R.id.meeting_content_edittext);
                EditText schoolView = (EditText) findViewById(R.id.meeting_school_edittext);
                EditText majorView = (EditText) findViewById(R.id.meeting_major_edittext);
                EditText countView = (EditText) findViewById(R.id.meeting_studentcount_edittext);

                String gender = isMale ? "male" : "female";
                String content = contentView.getText().toString();
                String schoolname = schoolView.getText().toString();
                String majorname = majorView.getText().toString();
                String studentcount = countView.getText().toString();

                try {
                    result = NetworkUtil.getInstance().checkIsLoginUser().writeMeetingContent(content, schoolname,
                            majorname, studentcount, gender);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(Boolean bool) {
                if (bool) {
                    AlertToast.success(getContext(), getString(R.string.success_board_write));
                    setResult(RESULT_OK);
                    finish();
                } else {
                    AlertToast.error(getContext(), getString(R.string.error_to_work));
                }
            }
        }.execute();
    }

    private Context getContext() {
        return this;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.meeting_write_gender:
                showGenderSelectDialog();
                break;
            case R.id.button:
                if (checkValidData()) {
                    writeMeetingBoard();
                }
                break;
        }
    }
}
