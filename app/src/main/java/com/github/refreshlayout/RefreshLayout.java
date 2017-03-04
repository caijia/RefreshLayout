package com.github.refreshlayout;

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
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import static android.support.v4.widget.ViewDragHelper.INVALID_POINTER;

/**
 * Created by cai.jia on 2017/2/10 0010
 */

public class RefreshLayout extends FrameLayout implements NestedScrollingParent, NestedScrollingChild {

    private static final float DRAG_RADIO = 0.5f;

    private static final int DEFAULT = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;
    private static final int REFRESH_COMPLETE = 5;
    private static final int ANIM_DEFAULT_DELAY = 500;
    public OnScrollListener onScrollListener;
    protected View headerView;
    protected View target;
    protected RefreshBehavior refreshBehavior;
    private int currentState = DEFAULT;
    private float scrollDistance;
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
    private ScrollerCompat mScroller;
    private VelocityTracker velocityTracker;
    private float minVelocity;
    private float maxVelocity;
    private int oldCurrY;
    /**
     * 刷新时是否固定头部
     */
    private boolean refreshingPinHeader;

    /**
     * 刷新完成后,延迟多久执行动画
     */
    private int completeDelay;

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

    private OnChildScrollUpCallback childScrollUpCallback;

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minVelocity = configuration.getScaledMinimumFlingVelocity();
        maxVelocity = configuration.getScaledMaximumFlingVelocity();

        nestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mScroller = ScrollerCompat.create(context);
        getAttributes(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getAttributes(context, attrs);
    }

    private void getAttributes(Context context, AttributeSet attrs) {
        TypedArray a = null;
        try {
            a = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
            dragRadio = a.getFloat(R.styleable.RefreshLayout_dragRadio, DRAG_RADIO);
            dragRadio = clampValue(0, 1, dragRadio);
            completeDelay = a.getInt(
                    R.styleable.RefreshLayout_completeDelayDuration, ANIM_DEFAULT_DELAY);
            refreshingPinHeader = a.getBoolean(
                    R.styleable.RefreshLayout_isRefreshingPinHeader, false);
            headerViewId = a.getResourceId(R.styleable.RefreshLayout_refreshHeaderId, -1);
            headerFrontTarget = a.getBoolean(R.styleable.RefreshLayout_isHeaderFrontTarget, true);
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
        if (refreshBehavior != null) {
            refreshBehavior.onStart(headerView.getMeasuredHeight());
        }
    }

    private void onActionMove(float deltaY) {
        if (isAnimRunning() || currentState == REFRESH_COMPLETE || refreshBehavior == null) {
            return;
        }

        scrollDistance += deltaY * (currentState == REFRESHING ? 1 : dragRadio);
        scrollDistance = clampValue(
                0, //min
                currentState == REFRESHING ? refreshDistance : dragRange, //max
                scrollDistance); //value

        scroll();

        if (refreshBehavior != null && currentState != REFRESHING) {
            refreshBehavior.onMove(scrollDistance, headerView.getMeasuredHeight());
        }
    }

    private void scroll() {
        if (refreshBehavior == null) {
            return;
        }

        if (onScrollListener == null
                || !onScrollListener.onScroll(scrollDistance, headerView, target)) {
            headerView.setTranslationY(scrollDistance);
            target.setTranslationY(scrollDistance);
        }
    }

    private void onActionUpOrCancel() {
        if (currentState == REFRESHING || currentState == REFRESH_COMPLETE || isAnimRunning()
                || refreshBehavior == null) {
            return;
        }
        computeState();
        stateMapAnimation();
    }

    private boolean isAnimRunning() {
        if (animator != null && (animator.isStarted() || animator.isRunning())) {
            return true;
        }
        return false;
    }

    private void stateMapAnimation() {
        switch (currentState) {
            case DEFAULT: {
                if (refreshBehavior != null) {
                    refreshBehavior.onReset();
                }

                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                if (animator != null) {
                    animator.cancel();
                }
                activePointerId = INVALID_POINTER;
                isBeginDragged = false;
                break;
            }

            case PULL_TO_REFRESH: {
                animator(scrollDistance, 0, 0);
                break;
            }

            case RELEASE_TO_REFRESH: {
                animator(scrollDistance, refreshDistance, 0);
                break;
            }

            case REFRESHING: {
                if (refreshBehavior != null) {
                    refreshBehavior.onRefreshing();
                }

                if (onRefreshListener != null) {
                    onRefreshListener.onRefresh();
                }
                break;
            }

            case REFRESH_COMPLETE: {
                if (refreshBehavior != null) {
                    refreshBehavior.onRefreshComplete();
                }
                animator(scrollDistance, 0, completeDelay);
                break;
            }
        }
    }

    private void animator(float start, float end, int delay) {
        if (start <= end || start <= 0) {
            scrollDistance = 0;
            scroll();
            computeState();
            stateMapAnimation();
            return;
        }

        if (animator != null && (animator.isStarted() || animator.isRunning())) {
            animator.cancel();
            return;
        }

        int distance = (int) (start - end);
        int customDuration = 0;
        if (refreshBehavior != null) {
            customDuration = refreshBehavior.animationDuration(distance);
        }
        int duration = customDuration == 0 ? pxToDp(distance) * 4 : customDuration;
        animator = ValueAnimator.ofFloat(start, end).setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scrollDistance = (float) animation.getAnimatedValue();
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
        if (scrollDistance == 0) {
            currentState = DEFAULT;

        } else if (scrollDistance > refreshDistance) {
            currentState = RELEASE_TO_REFRESH;

        } else if (scrollDistance == refreshDistance) {
            currentState = REFRESHING;

        } else if (scrollDistance < refreshDistance) {
            currentState = PULL_TO_REFRESH;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && target instanceof AbsListView)
                || (target != null && !ViewCompat.isNestedScrollingEnabled(target))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (refreshBehavior == null) {
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
        if (refreshBehavior == null) {
            target.layout(0, 0, target.getMeasuredWidth(), target.getMeasuredHeight());

        } else {
            if (!refreshBehavior.onLayoutChild(headerView, target)) {
                headerView.layout(0, -headerView.getMeasuredHeight(), headerView.getMeasuredWidth(), 0);
                target.layout(0, 0, target.getMeasuredWidth(), target.getMeasuredHeight());
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 1) {
            throw new RuntimeException("only one child");
        }
        target = getChildAt(0);

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
        if (childScrollUpCallback != null) {
            return childScrollUpCallback.canChildScrollUp(this, target);
        }
        return ViewCompat.canScrollVertically(target, -1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled() || canChildScrollUp() || refreshBehavior == null) {
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

                float diffX = x - initialMotionX;
                float diffY = y - initialMotionY;

                if (!isBeginDragged && Math.abs(diffY) > Math.abs(diffX)
                        && (diffY > touchSlop  //move down
                        || (!refreshingPinHeader && scrollDistance > 0 && -diffY > touchSlop))) {
                    isBeginDragged = true;
                }

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

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled() || canChildScrollUp() || refreshBehavior == null) {
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

                float deltaY = y - lastTouchY;

                float diffX = x - initialMotionX;
                float diffY = y - initialMotionY;

                if (!isBeginDragged && Math.abs(diffY) > Math.abs(diffX)) {
                    if (diffY > touchSlop) {
                        isBeginDragged = true;
                        deltaY = diffY - touchSlop;
                    }

                    if (!refreshingPinHeader && scrollDistance > 0 && -diffY > touchSlop) {
                        isBeginDragged = true;
                        deltaY = diffY + touchSlop;
                    }
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

    private void fling(int velocityY) {
        int startY = (int) (refreshDistance - scrollDistance);
        oldCurrY = startY;
        mScroller.fling(
                0, startY, //init value
                0, velocityY, //velocity
                0, 0, // x
                0, refreshDistance); //y
        if (mScroller.computeScrollOffset()) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            int dy = oldCurrY - mScroller.getCurrY();
            onActionMove(dy);
            oldCurrY = mScroller.getCurrY();
            ViewCompat.postInvalidateOnAnimation(this);

        } else {
            oldCurrY = 0;
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
            animator(0, refreshDistance, completeDelay);
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
        if (mScroller.isFinished()) {
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
        if (dy < 0 && !canChildScrollUp()) {
            onActionMove(Math.abs(dyUnconsumed));
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && scrollDistance > 0) {
            if (dy > scrollDistance) {
                consumed[1] = (int) (dy - scrollDistance);

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
            return scrollDistance > 0;
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

    public void setCompleteDelay(int completeDelay) {
        this.completeDelay = completeDelay;
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
            addView(headerView, headerFrontTarget ? 1 : 0);
        }
    }

    public void setHeaderViewId(@LayoutRes int headerViewId) {
        View headerView = LayoutInflater.from(getContext())
                .inflate(headerViewId, this, false);
        setHeaderView(headerView);
    }

    public interface OnRefreshListener {

        void onRefresh();
    }

    public interface OnScrollListener {

        /**
         * @param scrollY    滑动和fling的距离
         * @param headerView 头部View
         * @param target     内容View
         * @return true 表示不使用内部的滑动(headerView ,target)的逻辑
         */
        boolean onScroll(float scrollY, View headerView, View target);
    }

    public interface OnChildScrollUpCallback {
        boolean canChildScrollUp(RefreshLayout parent, View child);
    }

    public interface RefreshBehavior {

        void onStart(float headerViewHeight);

        void onMove(float scrollTop, float headerViewHeight);

        void onRefreshing();

        void onRefreshComplete();

        void onReset();

        int refreshDistance(int headerViewHeight);

        int dragRange(int headerViewHeight);

        boolean onLayoutChild(View headerView, View target);

        int animationDuration(@Px int distance);
    }

    public static class SimpleRefreshBehavior implements RefreshBehavior {

        @Override
        public void onStart(float headerViewHeight) {

        }

        @Override
        public void onMove(float scrollTop, float headerViewHeight) {

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
}
