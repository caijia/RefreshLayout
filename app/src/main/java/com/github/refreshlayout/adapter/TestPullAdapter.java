package com.github.refreshlayout.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.refreshlayout.R;

/**
 * Created by cai.jia on 2017/2/13 0013
 */
public class TestPullAdapter extends RecyclerView.Adapter<TestPullAdapter.TestPullVH> {

    @Override
    public TestPullVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_test_pull, parent, false);
        return new TestPullVH(view);
    }

    @Override
    public void onBindViewHolder(TestPullVH holder, int position) {
        holder.textView.setText("ITEM" + position);
    }

    @Override
    public int getItemCount() {
        return 15;
    }

    static class TestPullVH extends RecyclerView.ViewHolder {

        private TextView textView;

        public TestPullVH(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text_view);
        }
    }
}
