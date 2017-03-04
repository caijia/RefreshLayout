package com.github.refreshlayout;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.github.refreshlayout.adapter.TestPullAdapter;

public class MainActivity extends AppCompatActivity implements RefreshLayout.OnRefreshListener {

    RefreshLayout refreshLayout;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();

        refreshLayout = (RefreshLayout) findViewById(R.id.refresh_layout);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new TestPullAdapter());

//        refreshLayout.setHeaderViewId(R.layout.header);d
        refreshLayout.setOnRefreshListener(this);
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
