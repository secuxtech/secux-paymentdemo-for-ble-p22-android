package com.secuxtech.mysecuxpay.Activity;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.TextView;
import android.util.Pair;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.secuxtech.mysecuxpay.Adapter.HistoryListAdapter;

import com.secuxtech.mysecuxpay.Interface.AdapterItemClickListener;
import com.secuxtech.mysecuxpay.Interface.OnListScrollListener;
import com.secuxtech.mysecuxpay.Model.Setting;
import com.secuxtech.mysecuxpay.R;
import com.secuxtech.paymentkit.SecuXPaymentHistory;
import com.secuxtech.paymentkit.SecuXPaymentManager;
import com.secuxtech.paymentkit.SecuXServerRequestHandler;

import java.util.ArrayList;

public class PaymentHistoryActivity extends BaseActivity {

    HistoryListAdapter mAdapter;
    SecuXPaymentManager mPaymentManager = new SecuXPaymentManager();
    ArrayList<SecuXPaymentHistory> mPayHisArr = new ArrayList<>();

    SwipeRefreshLayout mSwiper;

    private Integer mCurrentHisPageIdx = 1;
    private Integer mLoadItemCount = 20;

    AdapterItemClickListener mItemClickListener = new AdapterItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            SecuXPaymentHistory history = mPayHisArr.get(position);
            Log.i(TAG, history.mDetailsUrl);

            if (history.mTransactionStatus.compareToIgnoreCase("established")!=0){
                showAlert("Failed transaction", "No transaction detail information.");
                return;
            }

            if (history.mDetailsUrl==null || history.mDetailsUrl.length()==0){
                showAlert("Transaction has no detail URL!", "");
                return;
            }


            Setting.getInstance().mLastPaymentHis = history;
            //Intent newIntent = new Intent(mContext, TokenTransferDetailsActivity.class);
            //newIntent.putExtra(TokenTransferDetailsActivity.TRANSACTION_HISTORY_DETAIL_URL, history.mDetailsUrl);
            Intent newIntent = new Intent(mContext, ReceiptActivity.class);
            startActivity(newIntent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView_history_list);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        //recyclerView.setOnScrollChangeListener(mScrollListener);
        loadPaymentHistory();

        mSwiper = findViewById(R.id.swiper_payment_history_list);
        mSwiper.setOnRefreshListener(mRefreshListener);
    }

    /*
    private RecyclerView.OnScrollChangeListener mScrollListener = new RecyclerView.OnScrollChangeListener() {


        @Override
        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY){

            Log.i(TAG, "dy=" + String.valueOf(scrollY));
        }
    };
    */

    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    final ArrayList<SecuXPaymentHistory> hisArr = new ArrayList<>();
                    if (!loadHistoryData(1, mLoadItemCount, hisArr)){
                        return;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (hisArr.size() > 0 && hisArr.get(0).mTransactionCode.compareTo(mPayHisArr.get(0).mTransactionCode)!=0) {
                                mPayHisArr = hisArr;
                                mAdapter.updateHistoryList(hisArr);
                                mAdapter.notifyDataSetChanged();
                            }

                            mSwiper.setRefreshing(false);
                        }
                    });
                }
            }).start();

        }
    };

    private void loadPaymentHistory(){
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView_history_list);
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (!loadHistoryData(1, mLoadItemCount, mPayHisArr)){
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mPayHisArr.size()>0){
                            mAdapter = new HistoryListAdapter(mContext, mPayHisArr, mItemClickListener);
                            recyclerView.setAdapter(mAdapter);

                            mAdapter.setOnListScrollListener(new OnListScrollListener(){
                                @Override
                                public void onBottomReached(int position){
                                    Log.i(TAG, "onBottomReached");
                                    loadMoreHistoryData();
                                }
                            });

                        }else{
                            TextView textviewNoHistory = findViewById(R.id.textView_nohistory);
                            textviewNoHistory.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                });

            }
        }).start();

    }


    private void loadMoreHistoryData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<SecuXPaymentHistory> hisArr = new ArrayList<>();
                if (!loadHistoryData(mCurrentHisPageIdx+1, mLoadItemCount, hisArr))
                    return;

                mCurrentHisPageIdx += 1;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int lastCount = mPayHisArr.size();

                        mPayHisArr.addAll(hisArr);
                        mAdapter.updateHistoryList(mPayHisArr);
                        mAdapter.notifyItemRangeInserted(lastCount, hisArr.size());
                    }
                });
            }
        }).start();
    }

    private boolean loadHistoryData(int pageIdx, int count, ArrayList<SecuXPaymentHistory> historyArr){
        Pair<Integer, String> ret = mPaymentManager.getPaymentHistory("", pageIdx, count, historyArr);
        if (ret.first== SecuXServerRequestHandler.SecuXRequestUnauthorized){
            showMessageInMain("Login timeout! Please login again!");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent newIntent = new Intent(mContext, LoginActivity.class);
                    startActivity(newIntent);
                }
            });

            return false;

        }else if (ret.first==SecuXServerRequestHandler.SecuXRequestFailed){
            showMessageInMain("Get payment history failed! Error: " + ret.second);

            if (ret.second.contains("No token")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent newIntent = new Intent(mContext, LoginActivity.class);
                        startActivity(newIntent);
                    }
                });
            }

            return false;
        }

        return true;
    }
}
