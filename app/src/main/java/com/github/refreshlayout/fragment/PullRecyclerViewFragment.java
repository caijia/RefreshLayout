package com.github.refreshlayout.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.refreshlayout.R;
import com.caijia.refreshlayout.RefreshLayout;
import com.github.refreshlayout.adapter.TestPullAdapter;

/**
 * Created by cai.jia on 2017/3/6 0006
 */

public class PullRecyclerViewFragment extends Fragment implements RefreshLayout.OnRefreshListener {

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
        View view = inflater.inflate(R.layout.fragment_pull_recyclerview, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (view != null) {
            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
            refreshLayout = (RefreshLayout) view.findViewById(R.id.refresh_layout);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(new TestPullAdapter());

//        refreshLayout.setHeaderViewId(R.layout.header);
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
        }, 4000);
    }
}
