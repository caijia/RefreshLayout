package com.github.refreshlayout.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.refreshlayout.R;
import com.caijia.refreshlayout.RefreshLayout;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Created by cai.jia on 2017/3/6 0006
 */

public class PullScrollViewFragment extends Fragment implements RefreshLayout.OnRefreshListener {

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
        View view = inflater.inflate(R.layout.fragment_pull_scrollview, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (view != null) {
            LinearLayout rootLl= (LinearLayout) view.findViewById(R.id.root_ll);

            refreshLayout = new RefreshLayout(getContext());
            refreshLayout.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            refreshLayout.setHeaderViewId(R.layout.header);
            refreshLayout.setTargetViewId(R.layout.view_scroll_view);
            refreshLayout.setOnRefreshListener(this);

            rootLl.addView(refreshLayout);
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
}
