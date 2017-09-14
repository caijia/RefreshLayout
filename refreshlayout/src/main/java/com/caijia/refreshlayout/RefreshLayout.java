/*
 * Copyright (C) 2017 caijia, caijia.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.caijia.refreshlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.annotation.Px;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Scroller;

import static android.support.v4.widget.ViewDragHelper.INVALID_POINTER;

/**
 * Created by cai.jia on 2017/2/10 0010
 */

public class RefreshLayout extends FrameLayout implements NestedScrollingParent, NestedScrollingChild {

    private static final float DRAG_RADIO = 0.4f;

    private static final int DEFAULT = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;
    private static final int REFRESH_COMPLETE = 5;
    private static final int CLOSE_HEADER_DELAY = 500;
    public OnScrollListener onScrollListener;
    protected View headerView;
    protected View targetView;
    protected RefreshBehavior refreshBehavior;
    private int currentState = DEFAULT;
    private float overScrollTop;
    private float dragRange;
    private ValueAnimator animator;
    private OnRefreshListener onRefreshListener;
    private int touchSlop;
    private float lastTouchY;
    private int activePointerId;
    private boolean isBeginDragged;
    private NestedScrollingChildHelper nestedScrollingChildHelper;
    private NestedScrollingParentHelper nestedScrollingParentHelper;
    private float initialMotionY;
    private float initialMotionX;
    private int[] mParentOffsetInWindow = new int[2];
    private int[] parentConsumed = new int[2];
    private Scroller mScroller;
    private VelocityTracker velocityTracker;
    private float minVelocity;
    private float maxVelocity;
    /**
     * 刷新时是否固定头部
     */
    private boolean refreshingPinHeader;

    /**
     * 刷新完成后,延迟多久关闭头部
     */
    private int closeHeaderDelay;

    /**
     * 拖动的比率
     */
    private float dragRadio;

    /**
     * 需要scroll多少距离才能触发刷新(刷新的临界值)
     */
    private int refreshDistance;

    /**
     * headerView是否在Target的上边,默认为true
     */
    private boolean headerFrontTarget;

    /**
     * headerView layout id;
     */
    private int headerViewId;

    /**
     * 是否可以刷新
     */
    private boolean refreshEnable;

    private OnChildScrollUpCallback childScrollUpCallback;
    private FlingRunnable flingRunnable;
    private OnFlingTargetListener flingTargetListener;
    private float beginDragDeltaY;
    private TargetFlingRunnable targetFlingRunnable;

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        getAttributes(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
        getAttributes(context, attrs);
    }

    private void init(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minVelocity = configuration.getScaledMinimumFlingVelocity();
        maxVelocity = configuration.getScaledMaximumFlingVelocity();

        nestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mScroller = new Scroller(context);
        flingRunnable = new FlingRunnable();
        targetFlingRunnable = new TargetFlingRunnable();
    }

    private void getAttributes(Context context, AttributeSet attrs) {
        TypedArray a = null;
        try {
            a = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
            dragRadio = a.getFloat(R.styleable.RefreshLayout_dragRadio, DRAG_RADIO);
            dragRadio = clampValue(0, 1, dragRadio);
            closeHeaderDelay = a.getInt(
                    R.styleable.RefreshLayout_closeHeaderDelay, CLOSE_HEADER_DELAY);
            refreshingPinHeader = a.getBoolean(
                    R.styleable.RefreshLayout_isRefreshingPinHeader, false);
            headerViewId = a.getResourceId(R.styleable.RefreshLayout_refreshHeaderId, -1);
            headerFrontTarget = a.getBoolean(R.styleable.RefreshLayout_isHeaderFrontTarget, true);
            refreshEnable = a.getBoolean(R.styleable.RefreshLayout_isRefreshEnable, true);
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    private void addVelocityTracker(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
    }

    private void recyclerVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void computeVelocity() {
        if (velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(1000);
        }
    }

    private void onActionDown() {
        if (hasHeader()) {
            refreshBehavior.onStart(headerView.getMeasuredHeight(), refreshDistance);
        }
    }

    private void onActionMove(float deltaY) {
        if (isAnimRunning() || currentState == REFRESH_COMPLETE || !hasHeader()) {
            return;
        }

        overScrollTop += deltaY * (currentState == REFRESHING ? 1 : dragRadio);
        overScrollTop = clampValue(
                0, //min
                currentState == REFRESHING ? refreshDistance : dragRange, //max
                overScrollTop); //value

        scroll();

        if (hasHeader() && currentState != REFRESHING) {
            refreshBehavior.onMove(overScrollTop, headerView.getMeasuredHeight(), refreshDistance);
        }
    }

    private void scroll() {
        if (!hasHeader() || !hasTargetView()) {
            return;
        }

        int scrollY = Math.round(overScrollTop);
        if (onScrollListener == null
                || !onScrollListener.onScroll(scrollY, headerView, targetView)) {
            headerView.setTranslationY(scrollY);
            targetView.setTranslationY(scrollY);
        }
    }

    private void onActionUpOrCancel() {
        if (currentState == REFRESHING || currentState == REFRESH_COMPLETE || isAnimRunning()
                || !hasHeader() || !refreshEnable) {
            return;
        }
        computeState();
        stateMapAnimation();
    }

    private boolean isAnimRunning() {
        return animator != null && (animator.isStarted() || animator.isRunning());
    }

    private void stateMapAnimation() {
        switch (currentState) {
            case DEFAULT: {
                if (hasHeader()) {
                    refreshBehavior.onReset();
                }

                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                if (animator != null) {
                    animator.cancel();
                }
                isBeginDragged = false;
                break;
            }

            case PULL_TO_REFRESH: {
                animator(overScrollTop, 0, 0);
                break;
            }

            case RELEASE_TO_REFRESH: {
                animator(overScrollTop, refreshDistance, 0);
                break;
            }

            case REFRESHING: {
                if (hasHeader()) {
                    refreshBehavior.onRefreshing();
                }

                if (onRefreshListener != null) {
                    onRefreshListener.onRefresh();
                }
                break;
            }

            case REFRESH_COMPLETE: {
                if (hasHeader()) {
                    refreshBehavior.onRefreshComplete();
                }
                animator(overScrollTop, 0, closeHeaderDelay);
                break;
            }
        }
    }

    private void animator(float start, float end, int delay) {
        if (start < 0) {
            overScrollTop = 0;
            scroll();
            computeState();
            stateMapAnimation();
            return;
        }

        if (animator != null && (animator.isStarted() || animator.isRunning())) {
            animator.cancel();
            return;
        }

        int distance = Math.abs(Math.round(start - end));
        int customDuration = 0;
        if (refreshBehavior != null) {
            customDuration = refreshBehavior.animationDuration(distance);
        }
        int duration = customDuration == 0 ? pxToDp(distance) * 4 : customDuration;
        animator = ValueAnimator.ofFloat(start, end).setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                overScrollTop = (float) animation.getAnimatedValue();
                scroll();
            }
        });
        animator.addListener(new SimpleAnimatorListener());
        animator.setStartDelay(delay);
        animator.start();
    }

    private int pxToDp(@Px int value) {
        return (int) Math.ceil(value / getResources().getDisplayMetrics().density);
    }

    private float clampValue(float min, float max, float value) {
        return Math.max(min, Math.min(max, value));
    }

    private void computeState() {
        if (overScrollTop == 0) {
            currentState = DEFAULT;

        } else if (overScrollTop > refreshDistance) {
            currentState = RELEASE_TO_REFRESH;

        } else if (overScrollTop == refreshDistance) {
            currentState = REFRESHING;

        } else if (overScrollTop < refreshDistance) {
            currentState = PULL_TO_REFRESH;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if (hasTargetView() && (android.os.Build.VERSION.SDK_INT < 21
                && targetView instanceof AbsListView)
                || (targetView != null && !ViewCompat.isNestedScrollingEnabled(targetView))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!hasHeader()) {
            return;
        }

        int headerHeight = headerView.getMeasuredHeight();
        int customDragRange = refreshBehavior.dragRange(headerHeight);
        dragRange = customDragRange == 0 ? headerHeight * 2 : customDragRange;
        int distance = refreshBehavior.refreshDistance(headerHeight);
        refreshDistance = distance <= 0 ? headerHeight : distance;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!hasHeader()) {
            super.onLayout(changed, l, t, r, b);

        } else {
            if (hasTargetView() && !refreshBehavior.onLayoutChild(headerView, targetView)) {
                FrameLayout.LayoutParams lp = (LayoutParams) targetView.getLayoutParams();
                int width = targetView.getMeasuredWidth();
                int left = getPaddingLeft() + lp.leftMargin;
                int right = left + width;

                int targetTop = getPaddingTop() + lp.topMargin;
                int targetBottom = targetTop + targetView.getMeasuredHeight();

                headerView.layout(left, -headerView.getMeasuredHeight(), right, 0);
                targetView.layout(left, targetTop, right, targetBottom);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 1) {
            throw new RuntimeException("only one child");
        }
        targetView = getChildAt(0);

        //add headerView
        if (headerViewId != -1) {
            headerView = LayoutInflater.from(getContext()).inflate(headerViewId, this, false);
            if (headerView == null || !(headerView instanceof RefreshBehavior)) {
                throw new RuntimeException("header is null or header don't implements RefreshBehavior");

            } else {
                addView(headerView, headerFrontTarget ? 1 : 0);
            }
        }

        if (headerView != null && headerView instanceof RefreshBehavior) {
            refreshBehavior = (RefreshBehavior) headerView;
        }
    }

    private boolean canChildScrollUp() {
        if (!hasTargetView()) {
            return false;
        }
        if (childScrollUpCallback != null) {
            return childScrollUpCallback.canChildScrollUp(this, targetView);
        }
        return ViewCompat.canScrollVertically(targetView, -1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!refreshEnable || !isEnabled() || canChildScrollUp() || !hasHeader()) {
            return false;
        }
        addVelocityTracker(ev);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                onMoveDown(ev);
                onActionDown();
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                onSecondaryPointerDown(ev);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int index = ev.findPointerIndex(activePointerId);
                if (index < 0) {
                    return false;
                }

                float x = ev.getX(index);
                float y = ev.getY(index);

                startDragging(x, y);

                lastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                onSecondaryPointerUp(ev);
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                recyclerVelocityTracker();
                isBeginDragged = false;
                activePointerId = INVALID_POINTER;
                break;
        }
        return isBeginDragged;
    }

    /**
     * 拦截横向滚动,当嵌套viewpager时需要配置.*/

    private boolean interceptHorizontalScroll;

    public void setInterceptHorizontalScroll(boolean interceptHorizontalScroll) {
        this.interceptHorizontalScroll = interceptHorizontalScroll;
    }

    private void startDragging(float x, float y) {
        float diffX = x - initialMotionX;
        float diffY = y - initialMotionY;
        if (!isBeginDragged && (!interceptHorizontalScroll || Math.abs(diffY) > Math.abs(diffX))) {
            if (diffY > touchSlop) {
                isBeginDragged = true;
                initialMotionY += touchSlop;
                beginDragDeltaY = diffY - touchSlop;
            }

            if (!refreshingPinHeader && overScrollTop > 0 && -diffY > touchSlop) {
                isBeginDragged = true;
                initialMotionY -= touchSlop;
                beginDragDeltaY = diffY + touchSlop;
            }
        }
    }

    private boolean hasHeader() {
        return refreshBehavior != null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!refreshEnable || !isEnabled() || canChildScrollUp() || !hasHeader()) {
            return false;
        }
        addVelocityTracker(ev);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                onMoveDown(ev);
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                onSecondaryPointerDown(ev);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int index = ev.findPointerIndex(activePointerId);
                if (index < 0) {
                    return false;
                }

                float x = ev.getX(index);
                float y = ev.getY(index);

                startDragging(x, y);

                float deltaY;
                if (beginDragDeltaY != 0) {
                    deltaY = beginDragDeltaY;
                    beginDragDeltaY = 0;

                } else {
                    deltaY = y - lastTouchY;
                }

                if (isBeginDragged) {
                    onActionMove(deltaY);
                }

                lastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                onSecondaryPointerUp(ev);
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                int index = ev.findPointerIndex(activePointerId);
                if (index < 0) {
                    return false;
                }
                computeVelocity();
                int velocityY = (int) velocityTracker.getYVelocity(activePointerId);
                boolean canFling = canFling(velocityY);
                if (!refreshingPinHeader && canFling) {
                    fling(-velocityY);

                } else {
                    onActionUpOrCancel();
                }
                activePointerId = INVALID_POINTER;
                isBeginDragged = false;
                break;
            }
        }

        return true;
    }

    /**
     * @param velocityY
     * @see FlingRunnable
     */
    private void fling(int velocityY) {
        if (!mScroller.isFinished()) {
            return;
        }

        mScroller.fling(
                0, 0, //init value
                0, velocityY, //velocity
                0, 0, // x
                Integer.MIN_VALUE, Integer.MAX_VALUE); //y
        if (mScroller.computeScrollOffset()) {
            flingRunnable.setDirection(velocityY > 0 ? FlingRunnable.UP : FlingRunnable.DOWN);
            ViewCompat.postOnAnimation(this, flingRunnable);
        }
    }

    /**
     * target滑动到顶部,然后在惯性滑动头部
     *
     * @param velocityY
     */
    private void targetFlingToTop(int velocityY) {
        if (!mScroller.isFinished()) {
            return;
        }

        mScroller.fling(
                0, 0, //init value
                0, velocityY, //velocity
                0, 0, // x
                Integer.MIN_VALUE, Integer.MAX_VALUE); //y
        if (mScroller.computeScrollOffset()) {
            ViewCompat.postOnAnimation(this, targetFlingRunnable);
        }
    }

    private boolean canFling(float velocity) {
        return Math.abs(velocity) > minVelocity && Math.abs(velocity) < maxVelocity
                && currentState == REFRESHING;
    }

    private void onMoveDown(MotionEvent ev) {
        initialMotionY = lastTouchY = ev.getY(0);
        initialMotionX = ev.getX(0);
        activePointerId = ev.getPointerId(0);
        isBeginDragged = false;
    }

    private void onSecondaryPointerDown(MotionEvent ev) {
        int index = ev.getActionIndex();
        activePointerId = ev.getPointerId(index);
        lastTouchY = ev.getY(index);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int index = ev.getActionIndex();
        if (ev.getPointerId(index) == activePointerId) {
            final int newIndex = index == 0 ? 1 : 0;
            activePointerId = ev.getPointerId(newIndex);
            lastTouchY = ev.getY(newIndex);
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (!refreshing) {
            currentState = REFRESH_COMPLETE;
            stateMapAnimation();

        } else {
            animator(0, refreshDistance, 0);
        }
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return nestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        nestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return nestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        nestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return nestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return nestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && (!refreshingPinHeader || currentState == DEFAULT)
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onStopNestedScroll(View target) {
        nestedScrollingParentHelper.onStopNestedScroll(target);
        //fling or scroll,当fling的时候,fling结束时才调用onActionUpOrCancel
        if (mScroller.isFinished() && overScrollTop > 0) {
            onActionUpOrCancel();
        }
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);
        int dy = dyUnconsumed + mParentOffsetInWindow[1];
        //move down
        if (dy < 0 && !canChildScrollUp() && refreshEnable) {
            onActionMove(Math.abs(dyUnconsumed));
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && overScrollTop > 0) {
            if (dy > overScrollTop) {
                consumed[1] = Math.round(overScrollTop);

            } else {
                consumed[1] = dy;
            }
            onActionMove(-consumed[1]);
        }

        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        //回到初始位置 velocity > 0 up
        if (velocityY > 0) {
            if (currentState == REFRESHING) {
                fling((int) velocityY);
            }
            return overScrollTop > 0;

        } else {
            //target滑动到顶部,然后在惯性滑动头部
            if (currentState == REFRESHING) {
                targetFlingToTop(Math.round(velocityY));
            }
        }

        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public int getNestedScrollAxes() {
        return nestedScrollingParentHelper.getNestedScrollAxes();
    }

    public void setOnChildScrollUpCallback(OnChildScrollUpCallback callback) {
        this.childScrollUpCallback = callback;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    public void setRefreshingPinHeader(boolean refreshingPinHeader) {
        this.refreshingPinHeader = refreshingPinHeader;
    }

    public void setCloseHeaderDelay(int closeHeaderDelay) {
        this.closeHeaderDelay = closeHeaderDelay;
    }

    public void setDragRadio(float dragRadio) {
        this.dragRadio = clampValue(0, 1, dragRadio);
    }

    public void setHeaderView(View headerView) {
        if (headerView != null && headerView instanceof RefreshBehavior) {
            //remove old header
            if (this.headerView != null && indexOfChild(this.headerView) != -1) {
                removeView(this.headerView);
                refreshBehavior = null;
                this.headerView = null;
            }
            this.headerView = headerView;
            refreshBehavior = (RefreshBehavior) headerView;
            addView(headerView, headerFrontTarget ? getChildCount() : 0);
        }
    }

    private boolean hasTargetView() {
        return targetView != null;
    }

    public void setTargetView(View targetView) {
        if (targetView == null) {
            return;
        }

        if (this.targetView != null && indexOfChild(this.targetView) != -1) {
            //remove old targetView
            removeView(this.targetView);
        }

        this.targetView = targetView;
        addView(this.targetView, headerFrontTarget ? 0 : getChildCount());
    }

    public void setTargetViewId(@LayoutRes int targetViewId) {
        View targetView = LayoutInflater.from(getContext())
                .inflate(targetViewId, this, false);
        setTargetView(targetView);
    }

    public void setHeaderViewId(@LayoutRes int headerViewId) {
        View headerView = LayoutInflater.from(getContext())
                .inflate(headerViewId, this, false);
        setHeaderView(headerView);
    }

    public void setRefreshEnable(boolean refreshEnable) {
        this.refreshEnable = refreshEnable;
    }

    public void setOnFlingTargetListener(OnFlingTargetListener onFlingTargetListener) {
        this.flingTargetListener = onFlingTargetListener;
    }

    private void flingTarget(int currVelocity) {
        if (!hasTargetView()) {
            return;
        }

        if (flingTargetListener != null && flingTargetListener.fling(targetView, currVelocity)) {
            return;
        }

        if (targetView instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) targetView;
            recyclerView.fling(0, currVelocity);

        } else if (targetView instanceof AbsListView) {
            AbsListView absListView = (AbsListView) targetView;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                absListView.fling(currVelocity);
            }

        } else if (targetView instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) targetView;
            scrollView.fling((int) mScroller.getCurrVelocity());

        } else if (targetView instanceof NestedScrollView) {
            NestedScrollView scrollView = (NestedScrollView) targetView;
            scrollView.fling(currVelocity);

        } else if (targetView instanceof WebView) {
            WebView webView = (WebView) targetView;
            webView.flingScroll(0, currVelocity);
        }
    }

    public interface OnRefreshListener {

        void onRefresh();
    }

    public interface OnScrollListener {

        /**
         * @param scrollY    滑动和fling的距离
         * @param headerView 头部View
         * @param target     内容View
         * @return true 表示不使用内部的滑动(headerView ,targetView)的逻辑
         */
        boolean onScroll(int scrollY, View headerView, View target);
    }

    public interface OnChildScrollUpCallback {
        boolean canChildScrollUp(RefreshLayout parent, View child);
    }

    public interface RefreshBehavior {

        void onStart(float headerViewHeight, int refreshDistance);

        void onMove(float scrollTop, float headerViewHeight, int refreshDistance);

        void onRefreshing();

        void onRefreshComplete();

        void onReset();

        int refreshDistance(int headerViewHeight);

        int dragRange(int headerViewHeight);

        boolean onLayoutChild(View headerView, View target);

        int animationDuration(@Px int distance);
    }

    public interface OnFlingTargetListener {

        /**
         * 当正在刷新时,向上fling,关闭头部后,target有时需要有fling行为
         * @param target   需要fling的View
         * @param velocity 速度
         * @return true表示消费了velocity
         */
        boolean fling(View target, int velocity);

    }

    public static class SimpleRefreshBehavior implements RefreshBehavior {

        @Override
        public void onStart(float headerViewHeight, int refreshDistance) {

        }

        @Override
        public void onMove(float scrollTop, float headerViewHeight, int refreshDistance) {

        }

        @Override
        public void onRefreshing() {

        }

        @Override
        public void onRefreshComplete() {

        }

        @Override
        public void onReset() {

        }

        @Override
        public int refreshDistance(int headerViewHeight) {
            return 0;
        }

        @Override
        public int dragRange(int headerViewHeight) {
            return 0;
        }

        @Override
        public boolean onLayoutChild(View headerView, View target) {
            return false;
        }

        @Override
        public int animationDuration(@Px int distance) {
            return 0;
        }
    }

    private class SimpleAnimatorListener implements Animator.AnimatorListener {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            computeState();
            stateMapAnimation();
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }

    private class TargetFlingRunnable implements Runnable {

        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                if (!canChildScrollUp()) {
                    mScroller.abortAnimation();
                    if (currentState == REFRESHING) {
                        fling(-Math.round(Math.round(mScroller.getCurrVelocity())));
                    }

                } else {
                    ViewCompat.postOnAnimation(RefreshLayout.this, this);
                }
            }
        }
    }

    private class FlingRunnable implements Runnable {

        static final int DOWN = 1;
        static final int UP = -1;
        private int direction;

        private int oldCurrY;

        void setDirection(int direction) {
            this.direction = direction;
        }

        private void reset() {
            oldCurrY = 0;
        }

        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                int dy = oldCurrY - mScroller.getCurrY();
                onActionMove(dy);
                oldCurrY = mScroller.getCurrY();

                switch (direction) {
                    case UP: {
                        if (overScrollTop <= 0) {
                            reset();
                            int currVelocity = Math.round(mScroller.getCurrVelocity());
                            mScroller.abortAnimation();
                            flingTarget(currVelocity);

                        } else {
                            ViewCompat.postOnAnimation(RefreshLayout.this, this);
                        }

                        break;
                    }

                    case DOWN: {
                        if (overScrollTop >= refreshDistance) {
                            reset();
                            mScroller.abortAnimation();

                        } else {
                            ViewCompat.postOnAnimation(RefreshLayout.this, this);
                        }
                        break;
                    }
                }

            } else {
                reset();
            }
        }
    }
}
