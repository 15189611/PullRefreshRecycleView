package com.charles.recycleview.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.charles.recycleview.LayoutManager.ChGridLayoutManager;
import com.charles.recycleview.LayoutManager.ChLinearLayoutManager;
import com.charles.recycleview.LayoutManager.ChStaggeredGridLayoutManager;
import com.charles.recycleview.Listener.LoadDataListener;
import com.charles.recycleview.Listener.OverScrollListener;
import com.charles.recycleview.R;

/**
 * Created by Charles on 2016/8/23.
 */

public class PullRefreshRecycleView extends RecyclerView implements Runnable {
    private Context mContext;
    private int dp1;
    private int headerImageHeight = -1; // 默认高度
    private int headerImageScaleHeight = -1; //
    private int headerImageMaxHeight = -1; // 最大高度(头部)
    private boolean isTouching = false; // 是否正在手指触摸的标识
    private HeaderView headerView; //头部刷新动画View

    private boolean pullToRefreshEnable = true; //是否设置刷新
    private boolean loadMoreEnable = true;

    private View mHead;
    private View mFoot;

    private BaseQuickAdapter mAdapter;
    private Handler mHandler = new MyHandler();
    private LoadDataListener mLoadDataListener;
    private boolean isLoadingData = false; // 是否正在加载数据

    public PullRefreshRecycleView(Context context) {
        this(context,null);
    }

    public PullRefreshRecycleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public PullRefreshRecycleView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private OverScrollListener mOverScrollListener = new OverScrollListener() {
        @Override
        public void overScrollBy(int dy) {
            // dy为拉伸过度时每毫秒拉伸的距离，正数表示向上拉伸多度，负数表示向下拉伸过度
            if ( pullToRefreshEnable && isTouching && (dy < 0 ) ){
                mHandler.obtainMessage(0, dy, 0, null).sendToTarget();
                onScrollChanged(0, 0, 0, 0);
            }
        }
    };

    private void init(Context context) {
        this.mContext = context;
        dp1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());
        headerImageMaxHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getContext().getResources().getDisplayMetrics());
        setOverScrollMode(OVER_SCROLL_NEVER);
        post(this);

    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.i("Charles","不可见");
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(mHandler);
    }

    @Override
    public void run() {
        LayoutManager manager = getLayoutManager();
        if (manager instanceof ChLinearLayoutManager) {
            ((ChLinearLayoutManager) manager).setOverScrollListener(mOverScrollListener);
        }else if (manager instanceof ChGridLayoutManager) {
            ((ChGridLayoutManager) manager).setOverScrollListener(mOverScrollListener);
        } else if (manager instanceof ChStaggeredGridLayoutManager) {
            ((ChStaggeredGridLayoutManager) manager).setOverScrollListener(mOverScrollListener);
        }
    }

    /**
     * 设置刷新和加载更多数据的监听
     */
    public void setLoadDataListener(LoadDataListener listener) {
        mLoadDataListener = listener;
    }

    /**
     * 刷新数据完成后调用，必须在UI线程中
     */
    public void refreshComplete() {
        isLoadingData = false;
        if (!loadMoreEnable) {
            mAdapter.removeAllFooterView();
            mAdapter.setNextLoadEnable(true);
        }
        headerImageHint();
    }
    /**
     * 加载更多完成后调用，必须在UI线程中
     */
    public void loadMoreComplete() {
        isLoadingData = false;
        mAdapter.loadComplete();
        setLoadMoreEnable(true);
    }

    /**
     * true代表还有下一页数据，false代表没有更多了
     */
    public void setLoadMoreEnable(boolean loadMoreEnable) {
        this.loadMoreEnable = loadMoreEnable;
        if (mAdapter != null) {
            mAdapter.setNextLoadEnable(loadMoreEnable);
        }
    }

    /**
     * 没有更多的数据了,必须调用
     */
    public void loadNoMoreView() {
        mAdapter.loadComplete();
        setLoadMoreEnable(false);
        mAdapter.addFooterView(LayoutInflater.from(mContext).inflate(R.layout.not_loading, this, false));
    }

    /**
     * 网络异常
     */
    public void showMoreFailedView(){
        Toast.makeText(mContext, "网络异常", Toast.LENGTH_LONG).show();
        mAdapter.showLoadMoreFailedView();
    }

    /**
     * 隐藏刷新动画(headView)
     */
    private void headerImageHint() {
        if (headerView == null) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(
                headerView.getLayoutParams().height, headerImageHeight);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                headerView.getLayoutParams().height = (int) animation.getAnimatedValue();
                headerView.requestLayout();
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                headerView.resetView();
            }
        });
        animator.start();
    }

    /**
     * 设置头部，不设置用默认的
     */
    public void setHeadView(View head){
        //TODO
        this.mHead = head;
    }

    /**
     * 设置底部，不设置用默认的
     */
    public void setFootView(View foot){
        //TODO
        this.mFoot = foot;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        mAdapter = (BaseQuickAdapter) adapter;

        if (headerView == null && pullToRefreshEnable) {
            // RecycleView新建头部
            RelativeLayout headerLayout = new RelativeLayout(mContext);
            headerLayout.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            headerView = new HeaderView(mContext);
            headerView.setMaxHeight(headerImageMaxHeight);
            headerLayout.addView(headerView, RelativeLayout.LayoutParams.MATCH_PARENT, dp1);
            setHeaderView(headerView);

            mAdapter.addHeaderView(headerLayout);  //此时的头部是隐藏的，添加到适配器的-1位置
        }

        if (loadMoreEnable) {
            mAdapter.setLoadingView(LayoutInflater.from(mContext).inflate(R.layout.def_loading, this, false));
        }

        mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {  //更多监听回调
            @Override
            public void onLoadMoreRequested() {
                if(mLoadDataListener != null){
                    mLoadDataListener.onLoadMore();
                }
            }
        });

        super.setAdapter(adapter);
    }

    public void setPullToRefreshEnable(boolean pullToRefreshEnable) {
        this.pullToRefreshEnable = pullToRefreshEnable;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (headerView == null) return;
        View view = (View) headerView.getParent();
        // 上推的时候减小高度至默认高度
        if (view.getTop() < 0 && headerView.getLayoutParams().height > headerImageHeight) {
            headerView.getLayoutParams().height += view.getTop();
            mHandler.obtainMessage(0, view.getTop(), 0, view).sendToTarget();
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
    }

    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            isTouching = true;
        }
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouching = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                isTouching = false;
                if (headerView.getLayoutParams().height > headerImageHeight) {
                    if (headerView != null && headerView.getLayoutParams().height > headerImageMaxHeight && mLoadDataListener != null  && !isLoadingData) {
                        refresh();
                        break;
                    }
                }

                headerImageHint();
                break;
        }

        return super.onTouchEvent(ev);
    }

    /**
     * 一进来就刷新调用此方法
     */
    public void forceRefresh() {
        if (headerView == null) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(
                headerView.getLayoutParams().height, headerImageMaxHeight);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                headerView.getLayoutParams().height = (int) animation.getAnimatedValue();
                headerView.requestLayout();
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                refresh();
            }
        });
        animator.start();
    }

    // 刷新
    private void refresh() {
        isLoadingData = true;
        if (headerView != null && mLoadDataListener != null) {
            headerView.startRefresh();
            mLoadDataListener.onRefresh();
        }
    }

    /**
     * 设置头部拉伸图片,头部中的背景ImageView
     */
    public void setHeaderView(HeaderView headerView) {
        this.headerView = headerView;
        headerImageHeight = headerView.getHeight();
        // 防止第一次拉伸的时候headerImage.getLayoutParams().height = 0
        if (headerImageHeight <= 0) {
            headerImageHeight = headerView.getLayoutParams().height;
        } else {
            this.headerView.getLayoutParams().height = headerImageHeight;
        }

    }

    private  class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    updateViewSize(msg);
                    break;
            }
        }
    }
    private  void updateViewSize (Message msg){
        // 重新设置View的宽高
        if (msg.obj != null) {
            headerView.getLayoutParams().height += msg.arg1;
            View view = ((View) msg.obj);
            view.layout(view.getLeft(), 0, view.getRight(), view.getBottom());
        } else {
            // 实现类似弹簧的阻力效果，拉的越长就越难拉的动
            headerImageScaleHeight = headerView.getLayoutParams().height
                    - headerImageHeight;

            if (headerImageScaleHeight > (headerImageMaxHeight - headerImageHeight) / 3) {
                headerView.getLayoutParams().height -= msg.arg1 / 3 * 2;
            } else if (headerImageScaleHeight > (headerImageMaxHeight - headerImageHeight) / 3 * 2) {
                headerView.getLayoutParams().height -= msg.arg1 / 3 * 3 ;
            } else {
                headerView.getLayoutParams().height -= msg.arg1 / 3 * 1.5 ;
            }
        }
        headerView.requestLayout();
    }

}
