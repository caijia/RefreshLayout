package com.github.refreshlayout;

/**
 * Created by cai.jia on 2017/2/10 0010
 */

public interface RefreshTrigger {

    void onStart(float headerViewHeight);

    void onMove(float scrollTop, float headerViewHeight);

    void onRefreshing();

    void onRefreshComplete();

    void onReset();

    int refreshDistance(int headerViewHeight);

    int dragRange(int headerViewHeight);
}
