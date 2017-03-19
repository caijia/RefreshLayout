package com.github.refreshlayout.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.github.refreshlayout.R;
import com.github.refreshlayout.RefreshLayout;
import com.github.refreshlayout.adapter.TestListAdapter;

import java.util.Locale;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Created by cai.jia on 2017/3/6 0006
 */

public class PullListViewFragment extends Fragment implements RefreshLayout.OnRefreshListener {

    RefreshLayout refreshLayout;
    Handler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pull_listview, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (view != null) {
            ListView listView = (ListView) view.findViewById(R.id.list_view);

            //add header
            View header = LayoutInflater.from(getContext())
                    .inflate(R.layout.view_list_header, listView, false);
            ViewPager viewPager = (ViewPager) header.findViewById(R.id.view_pager);
            viewPager.setAdapter(new MyPagerAdapter());
            listView.addHeaderView(header);

            refreshLayout = (RefreshLayout) view.findViewById(R.id.refresh_layout);
            listView.setAdapter(new TestListAdapter(getContext()));
            refreshLayout.setOnRefreshListener(this);
        }
    }

    @Override
    public void onRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
            }
        }, 2000);
    }

    private class MyPagerAdapter extends PagerAdapter{

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TextView textView = new TextView(getContext());
            textView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            textView.setText(String.format(Locale.CHINESE,"ViewPager Item Position = %d", position));
            textView.setTextSize(18);
            textView.setGravity(Gravity.CENTER);
            container.addView(textView);
            return textView;
        }
    }
}
