package com.github.refreshlayout.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.github.refreshlayout.R;
import com.caijia.refreshlayout.RefreshLayout;

/**
 * Created by cai.jia on 2017/3/6 0006
 */

public class PullWebViewFragment extends Fragment implements RefreshLayout.OnRefreshListener {

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
        View view = inflater.inflate(R.layout.fragment_pull_webview, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (view != null) {
            refreshLayout = (RefreshLayout) view.findViewById(R.id.refresh_layout);
            final WebView webView = (WebView) view.findViewById(R.id.web_view);
            webView.loadUrl("https://www.github.com/caijia/RefreshLayout");
            webView.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    webView.loadUrl(url);
                    return true;
                }
            });

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
        }, 2000);
    }
}
