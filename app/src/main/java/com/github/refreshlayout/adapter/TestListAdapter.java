package com.github.refreshlayout.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.refreshlayout.R;

/**
 * Created by cai.jia on 2017/3/6 0006
 */

public class TestListAdapter extends ArrayAdapter<String> {

    public TestListAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public int getCount() {
        return 15;
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return "ITEM" + position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_test_pull, parent, false);
            holder = new ViewHolder();
            holder.textView = (TextView) convertView.findViewById(R.id.text_view);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.textView.setText(getItem(position));
        return convertView;
    }

    private static class ViewHolder {

        TextView textView;
    }
}
