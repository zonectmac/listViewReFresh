package com.example.administrator.listviewrefresh.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.administrator.listviewrefresh.App;
import com.example.administrator.listviewrefresh.R;
import com.example.administrator.listviewrefresh.adapter.SwipeRecyclerViewAdapter;
import com.example.administrator.listviewrefresh.model.BannerModel;
import com.example.administrator.listviewrefresh.model.RefreshModel;
import com.example.administrator.listviewrefresh.util.ThreadUtil;
import com.example.administrator.listviewrefresh.widget.Divider;

import java.util.List;

import cn.bingoogolapple.androidcommon.adapter.BGAOnItemChildClickListener;
import cn.bingoogolapple.androidcommon.adapter.BGAOnItemChildLongClickListener;
import cn.bingoogolapple.androidcommon.adapter.BGAOnRVItemClickListener;
import cn.bingoogolapple.androidcommon.adapter.BGAOnRVItemLongClickListener;
import cn.bingoogolapple.bgabanner.BGABanner;
import cn.bingoogolapple.refreshlayout.BGANormalRefreshViewHolder;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout.BGARefreshLayoutDelegate;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListViewRefresh extends BaseActiivty implements BGARefreshLayoutDelegate,BGAOnRVItemClickListener,BGAOnRVItemLongClickListener,BGAOnItemChildClickListener,BGAOnItemChildLongClickListener{
    private BGARefreshLayout refreshLayout;
    private BGABanner mBanner;
    private RecyclerView mDataRv;
    private SwipeRecyclerViewAdapter mAdapter;
    private int mNewPageNumber = 0;
    private int mMorePageNumber = 0;
    @Override
    protected void initView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_recyclerview);
        refreshLayout= getViewById(R.id.refreshLayout);
        mBanner= getViewById(R.id.banner);
        mDataRv= getViewById(R.id.data);
    }

    @Override
    protected void setListener() {
        refreshLayout.setDelegate(this);
        mAdapter = new SwipeRecyclerViewAdapter(mDataRv);
        mAdapter.setOnRVItemClickListener(this);
        mAdapter.setOnRVItemLongClickListener(this);
        mAdapter.setOnItemChildClickListener(this);
        mAdapter.setOnItemChildLongClickListener(this);
        mDataRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
               if(RecyclerView.SCROLL_STATE_DRAGGING==newState){
                   mAdapter.closeOpenedSwipeItemLayoutWithAnim();
               }
            }
        });
        getViewById(R.id.retweet).setOnClickListener(this);
        getViewById(R.id.comment).setOnClickListener(this);
        getViewById(R.id.praise).setOnClickListener(this);
    }

    @Override
    protected void processLogic(Bundle savedInstanceState) {
        refreshLayout.setRefreshViewHolder(new BGANormalRefreshViewHolder(mApp,true));
        initBanner();
        mDataRv.addItemDecoration(new Divider(this));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mDataRv.setLayoutManager(linearLayoutManager);

        mDataRv.setAdapter(mAdapter);

        mEngine.loadInitDatas().enqueue(new Callback<List<RefreshModel>>() {
            @Override
            public void onResponse(Call<List<RefreshModel>> call, Response<List<RefreshModel>> response) {
                mAdapter.setData(response.body());
            }

            @Override
            public void onFailure(Call<List<RefreshModel>> call, Throwable t) {
            }
        });
    }
    private void initBanner() {
        mBanner.setAdapter(new BGABanner.Adapter() {
            @Override
            public void fillBannerItem(BGABanner banner, View view, Object model, int position) {
                Glide.with(banner.getContext()).load(model).placeholder(R.mipmap.holder).error(R.mipmap.holder).dontAnimate().thumbnail(0.1f).into((ImageView) view);
            }
        });

        App.getInstance().getEngine().getBannerModel().enqueue(new Callback<BannerModel>() {
            @Override
            public void onResponse(Call<BannerModel> call, Response<BannerModel> response) {
                BannerModel bannerModel = response.body();
                mBanner.setData(R.layout.view_image, bannerModel.imgs, bannerModel.tips);
            }

            @Override
            public void onFailure(Call<BannerModel> call, Throwable t) {
            }
        });
    }
    @Override
    public void onBGARefreshLayoutBeginRefreshing(final BGARefreshLayout refreshLayout) {
        mNewPageNumber++;
        if (mNewPageNumber>4){
            refreshLayout.endRefreshing();
            showToast("没有最新数据了");
            return;
        }
        showLoadingDialog();
        //加载数据
        mEngine.loadNewData(mNewPageNumber).enqueue(new Callback<List<RefreshModel>>() {
            @Override
            public void onResponse(Call<List<RefreshModel>> call, final Response<List<RefreshModel>> response) {
                ThreadUtil.runInUIThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.endRefreshing();//先结束再刷新
                            dismissLoadingDialog();
                            mAdapter.addNewData(response.body());
                            mDataRv.smoothScrollToPosition(0);//放在最上面
                    }
                },1000);

            }

            @Override
            public void onFailure(Call<List<RefreshModel>> call, Throwable t) {
                refreshLayout.endRefreshing();//先结束再刷新
                dismissLoadingDialog();
            }
        });
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(final BGARefreshLayout refreshLayout) {
        mMorePageNumber++;
        if (mMorePageNumber>4){
            refreshLayout.endLoadingMore();
            showToast("没有更多数据了");
            return false;
        }
        showLoadingDialog();
        mEngine.loadMoreData(mMorePageNumber).enqueue(new Callback<List<RefreshModel>>() {
            @Override
            public void onResponse(Call<List<RefreshModel>> call, final Response<List<RefreshModel>> response) {
                ThreadUtil.runInUIThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.endLoadingMore();
                        dismissLoadingDialog();
                        mAdapter.addMoreData(response.body());
                    }
                },2000);

            }

            @Override
            public void onFailure(Call<List<RefreshModel>> call, Throwable t) {
                refreshLayout.endLoadingMore();
                dismissLoadingDialog();
            }
        });

        return true;
    }

    @Override
    public void onRVItemClick(ViewGroup parent, View itemView, int position) {
        showToast("点击了条目 " + mAdapter.getItem(position).title);
    }

    @Override
    public boolean onRVItemLongClick(ViewGroup parent, View itemView, int position) {
        showToast("长按了条目 " + mAdapter.getItem(position).title);
        return true;
    }

    @Override
    public void onItemChildClick(ViewGroup parent, View childView, int position) {
        if (childView.getId() == R.id.tv_item_swipe_delete) {
            mAdapter.removeItem(position);
        }
    }

    @Override
    public boolean onItemChildLongClick(ViewGroup parent, View childView, int position) {
        if (childView.getId() == R.id.tv_item_swipe_delete) {
            showToast("长按了删除 " + mAdapter.getItem(position).title);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.retweet) {
            showToast("点击了转发");
            Intent intent=new Intent(getApplicationContext(),Login.class);
            startActivity(intent);
        } else if (v.getId() == R.id.comment) {
            showToast("点击了评论");
        } else if (v.getId() == R.id.praise) {
            showToast("点击了赞");
        }
    }
}
