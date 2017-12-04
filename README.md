# RefreshLayout
自定义头部,支持任意布局的下拉刷新。

# Android Studio 引入
1. 在Project的build.gradle文件里面加入
```
allprojects {
    repositories {
        google()
        jcenter()
        maven {
            url  "https://dl.bintray.com/caijialib/caijiaLibray"
        }
    }
}
```

2. 在Module的build.gradle文件里面加入
```
compile 'com.caijia:refreshlayout:1.0.1'
```
#Useage
1. 布局文件
```
<com.caijia.refreshlayout.RefreshLayout
        android:id="@+id/refresh_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:refreshHeaderId="@layout/common_view_home_refresh_header">

        <android.support.v7.widget.RecyclerView
            android:id="@+id/rv_music"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

    </com.caijia.refreshlayout.RefreshLayout>
```

RefreshLayout自定义属性


属性 | 含义 | 默认值 | 是否必填
--- | --- | --- | --- 
closeHeaderDelay | 当刷新完成，延时多久关闭头部 | 500ms | 否
dragRadio | 下拉的阻尼参数 | 0.4f(如果手指移动100px，那么下拉40px) | 否
isHeaderFrontTarget | 头部是否在滚动内容的上面 | true | 否
isRefreshEnable | 是否可以刷新 | true | 否
isRefreshingPinHeader | 刷新时头部是否固定 | false | 否
refreshHeaderId | 头部的自定义layout | 无 | 是


2. 头部的自定义，默认是ClassicRefreshHeaderView，当传refreshHeaderId里面的布局必须实现RefreshLayout.RefreshBehavior接口
```
public interface RefreshBehavior {

        void onStart(float headerViewHeight, int refreshDistance);

        void onMove(float move, float headerViewHeight, int refreshDistance);

        void onRefreshing();

        void onRefreshComplete();

        void onReset();

        int refreshDistance(int headerViewHeight);

        int dragRange(int headerViewHeight);

        boolean onLayoutChild(View headerView, View target);

        int animationDuration(@Px int distance);
    }
```

1. 当下拉开始时调用
```
void onStart(float headerViewHeight, int refreshDistance);
```

参数 | 含义
--- | ---
headerViewHeight | 自定义头部的高度
refreshDistance  | 刷新的距离，当下拉达到此距离时，松开手指开始刷新，刷新临界值


2. 移动回调

```
void onMove(float move, float headerViewHeight, int refreshDistance);
```

参数 | 含义
--- | ---
move | 移动的距离
headerViewHeight | 自定义头部的高度
refreshDistance  | 刷新的距离，当下拉达到此距离时，松开手指开始刷新，刷新临界值

3. 正在刷新回调

```
void onRefreshing();
```

4.刷新完成,刷新完成并不是回到初始位置，头部还没有缩回。
```
void onRefreshComplete();
```
5. 重置，回调了初始位置，头部缩回。
```
void onReset();
```

6.调整刷新临界值,这个值默认是头部高度，可以自定义这个值，
```
int refreshDistance(int headerViewHeight);
```

7.调整下拉的范围，最大下拉只能到达这个值
```
int dragRange(int headerViewHeight)
```

8.调整头部布局，和内容布局的位置，默认是上下排列
```
boolean onLayoutChild(View headerView, View target)
```

9.调整头部缩回的速度
```
int animationDuration(@Px int distance)
```
