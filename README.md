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
