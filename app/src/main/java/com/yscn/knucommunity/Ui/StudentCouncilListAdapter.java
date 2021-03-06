package com.yscn.knucommunity.Ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.yscn.knucommunity.Items.StudentCouncilListItems;
import com.yscn.knucommunity.R;
import com.yscn.knucommunity.Util.ApplicationUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

/**
 * Created by GwonHyeok on 14. 11. 3..
 */
public class StudentCouncilListAdapter extends ArrayAdapter<StudentCouncilListItems> {

    public StudentCouncilListAdapter(Context context, int resource, ArrayList<StudentCouncilListItems> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
        View view = convertView;
        AdapterHolder holder;
        if (view == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            view = inflater.inflate(R.layout.ui_studentcouncillist, parent, false);

            holder = new AdapterHolder();
            holder.title = (TextView) view.findViewById(R.id.ui_studentcouncil_title);
            holder.sumary = (TextView) view.findViewById(R.id.ui_studentcouncil_summary);
            view.setTag(holder);
            ApplicationUtil.getInstance().setTypeFace(view);
        } else {
            holder = (AdapterHolder) view.getTag();
        }
        StudentCouncilListItems object = getItem(position);
        try {
            holder.title.setText(URLDecoder.decode(object.getTitle(), "UTF-8"));
            holder.sumary.setText(URLDecoder.decode(object.getSummary(), "UTF-8"));
        } catch (UnsupportedEncodingException ignore) {

        }
        return view;
    }

    private class AdapterHolder {
        TextView title, sumary;
    }
}
