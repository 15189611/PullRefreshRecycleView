package com.charles.recycleview;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.charles.recycleview.LayoutManager.ChLinearLayoutManager;
import com.charles.recycleview.Listener.LoadDataListener;
import com.charles.recycleview.View.PullRefreshRecycleView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler = new Handler();
    private List<String> list=new ArrayList<>();
    private BaseQuickAdapter<String> adapter;
    private PullRefreshRecycleView recycleView;
    private boolean flag=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        show();
    }

    private void  show(){
        recycleView= (PullRefreshRecycleView) findViewById(R.id.ch_rv);

        ChLinearLayoutManager layoutManager = new ChLinearLayoutManager(this);

        recycleView.setLayoutManager(layoutManager);

        adapter=new BaseQuickAdapter<String>(R.layout.rv_item,list) {
            @Override
            protected void convert(BaseViewHolder helper, String item) {
                helper.setText(R.id.tv_rv,item);
            }
        };

        recycleView.setAdapter(adapter);

        recycleView.setLoadDataListener(new LoadDataListener() {
            @Override
            public void onRefresh() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                        recycleView.refreshComplete();
                    }
                }, 2000);
            }

            @Override
            public void onLoadMore() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 刷新完成后调用，必须在UI线程中
                        addData();
                        if(!flag){
                            recycleView.loadMoreComplete();
                        }else {
                            recycleView.loadNoMoreView();   //如果没有数据了，就调用此方法
                        }

                    }
                }, 2000);
            }
        });
    }

    /**
     * 添加数据
     */
    private void addData() {
        if(list.size() >80){
            flag=true;
            return;
        }
        for (int i = 0; i < 13; i++) {
            list.add("条目  " + (list.size() + 1));
        }
    }

    private void initData() {
        list.clear();
        flag=false;
        for (int i = 0 ; i < 50 ; i++){
            list.add("i=="+i);
        }
    }

}
