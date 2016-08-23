package com.charles.recycleview.Listener;

/**
 * 刷新和加载更多数据的监听接口
 */
public interface LoadDataListener {
    void onRefresh();

    void onLoadMore();
}
