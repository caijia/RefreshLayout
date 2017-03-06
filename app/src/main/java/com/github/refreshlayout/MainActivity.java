package com.github.refreshlayout;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.github.refreshlayout.fragment.PullListViewFragment;
import com.github.refreshlayout.fragment.PullNestedScrollViewFragment;
import com.github.refreshlayout.fragment.PullRecyclerViewFragment;
import com.github.refreshlayout.fragment.PullScrollViewFragment;
import com.github.refreshlayout.fragment.PullWebViewFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String[] tabTitleArray = {
            "RecyclerView", "ListView", "NestedScrollView", "ScrollView","WebView"
    };

    List<Fragment> fragmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentList = new ArrayList<>();
        fragmentList.add(new PullRecyclerViewFragment());
        fragmentList.add(new PullListViewFragment());
        fragmentList.add(new PullNestedScrollViewFragment());
        fragmentList.add(new PullScrollViewFragment());
        fragmentList.add(new PullWebViewFragment());

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(pager);
    }

    private class MyPagerAdapter extends FragmentPagerAdapter{

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitleArray[position];
        }
    }


}
