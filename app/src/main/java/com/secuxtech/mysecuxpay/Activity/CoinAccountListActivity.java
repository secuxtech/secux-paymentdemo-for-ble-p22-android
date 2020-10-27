package com.secuxtech.mysecuxpay.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.secuxtech.mysecuxpay.Adapter.CoinAccountListAdapter;
import com.secuxtech.mysecuxpay.Adapter.HistoryListAdapter;
import com.secuxtech.mysecuxpay.Adapter.TokenTransHistoryAdapter;
import com.secuxtech.mysecuxpay.Dialog.CoinTokenListDialog;
import com.secuxtech.mysecuxpay.Dialog.CommonAlertDialog;
import com.secuxtech.mysecuxpay.Interface.AdapterItemClickListener;
import com.secuxtech.mysecuxpay.Model.CoinTokenAccount;
import com.secuxtech.mysecuxpay.Model.Setting;
import com.secuxtech.mysecuxpay.R;
import com.secuxtech.mysecuxpay.Utility.AccountUtil;
import com.secuxtech.mysecuxpay.Utility.CommonProgressDialog;
import com.secuxtech.paymentkit.SecuXAccountManager;
import com.secuxtech.paymentkit.SecuXPaymentHistory;
import com.secuxtech.paymentkit.SecuXServerRequestHandler;

import java.util.ArrayList;


public class CoinAccountListActivity extends BaseActivity implements PopupMenu.OnMenuItemClickListener {

    SecuXAccountManager             mAccountManager = new SecuXAccountManager();
    CoinAccountListAdapter          mAdapter;
    ArrayList<CoinTokenAccount>     mTokenAccountArray = new ArrayList<>();
    Button                          mButtonDelete;
    Button                          mButtonAdd;

    CoinTokenListDialog             mCoinTokenListDlg = new CoinTokenListDialog();

    SwipeRefreshLayout              mSwiper;

    RecyclerView                    mRecyclerView;
    boolean                         mReloadAccount = false;

    private AdapterItemClickListener mItemClickListener = new AdapterItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            CoinTokenAccount account = mTokenAccountArray.get(position);
            Log.i(TAG, account.mAccountName + " " + account.mCoinType + " " + account.mToken);

            if (mAdapter.getSelectAccountList().size()>0){
                mButtonDelete.setVisibility(View.VISIBLE);
            }else{
                mButtonDelete.setVisibility(View.INVISIBLE);
            }

            /*
            Intent newIntent = new Intent(mContext, TokenTransHistoryActivity.class);
            newIntent.putExtra(TokenTransHistoryActivity.TRANSACTION_HISTORY_COINTYPE, account.mCoinType);
            newIntent.putExtra(TokenTransHistoryActivity.TRANSACTION_HISTORY_TOKEN, account.mToken);
            startActivity(newIntent);

             */
        }
    };


    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    loadAccounts();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwiper.setRefreshing(false);
                        }
                    });
                }
            }).start();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mShowBackButton = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_account_list);

        mButtonDelete = findViewById(R.id.button_account_list_delete);
        mButtonAdd = findViewById(R.id.floating_button_add_account);

        mSwiper = findViewById(R.id.swipe_account_list);
        mSwiper.setOnRefreshListener(mRefreshListener);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_acclist_accounts);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));

        mReloadAccount = false;
        if (AccountUtil.getCoinTokenAccounts().size() == 0) {
            showProgress("Loading...");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    loadAccounts();

                /*
                boolean findLBRAccount = false;
                for(CoinTokenAccount account : mTokenAccountArray){
                    if (account.mCoinType.compareTo("LBR")==0){
                        findLBRAccount = true;
                        break;
                    }
                }


                if (!findLBRAccount){
                    Pair<Integer, String> ret = mAccountManager.coinAccountOperation("LBR", "", "", "Add");
                    if (ret.first == SecuXServerRequestHandler.SecuXRequestOK) {
                        Log.i(TAG, "Create LBR account done");
                        loadAccounts();
                    }else{
                        showMessageInMain("Create LBR account failed!");
                    }
                }

                 */

                    hideProgressInMain();
                }
            }).start();
        }else{
            mTokenAccountArray = AccountUtil.getCoinTokenAccounts();
            if (mAdapter == null) {
                mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_acclist_accounts);
                mAdapter = new CoinAccountListAdapter(mContext, mTokenAccountArray, mItemClickListener);
                mRecyclerView.setAdapter(mAdapter);
            }else{
                mReloadAccount = true;
            }

            if (showAddAccountButton()) {
                mButtonAdd.setVisibility(View.VISIBLE);
            }else{
                mButtonAdd.setVisibility(View.INVISIBLE);
            }
        }

        BottomNavigationView navigationView = findViewById(R.id.navigation_main);
        navigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mReloadAccount && mAdapter!=null){
            mAdapter.updateAccountList(AccountUtil.getCoinTokenAccounts());
            mAdapter.notifyDataSetChanged();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        mCoinTokenListDlg.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCoinTokenListDlg.dismiss();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCoinTokenListDlg.dismiss();
    }

    public void onMenuButtonClick(View v){

        Context wrapper = new ContextThemeWrapper(this, R.style.AccountOptPopupMenuStyle);
        PopupMenu popup = new PopupMenu(wrapper, v);

        //PopupMenu popup = new PopupMenu(this, v);

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_account_operation_navbar, popup.getMenu());
        popup.setOnMenuItemClickListener(this::onMenuItemClick);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            /*
            case R.id.navigation_account_operation_transactions:
                //Toast.makeText(this, "transaction history", Toast.LENGTH_SHORT).show();
                Intent hisIntent = new Intent(mContext, TokenTransHistoryActivity.class);
                hisIntent.putExtra(TokenTransHistoryActivity.TRANSACTION_HISTORY_COINTYPE, mAdapter.getSelectAccountList().get(0).mCoinType);
                hisIntent.putExtra(TokenTransHistoryActivity.TRANSACTION_HISTORY_TOKEN, mAdapter.getSelectAccountList().get(0).mToken);
                startActivity(hisIntent);
                break;

             */
            case R.id.navigation_account_operation_receive:
                //Toast.makeText(this, "receive", Toast.LENGTH_SHORT).show();
                if (mAdapter.getSelectAccountList().size() > 0) {
                    ReceiveActivity.mAccount = mAdapter.getSelectAccountList().get(0);
                    Intent recvIntent = new Intent(mContext, ReceiveActivity.class);
                    startActivity(recvIntent);
                }
                break;
            //case R.id.navigation_account_operation_send:
            //    Toast.makeText(this, "send", Toast.LENGTH_SHORT).show();
            //    break;
            case R.id.navigation_account_operation_delete:
                //Toast.makeText(this, "delete", Toast.LENGTH_SHORT).show();
                onDeleteAccountButtonClick(mButtonDelete);
                break;
        }
        return false;
    }

    public void onDeleteAccountButtonClick(View v){

        ArrayList<CoinTokenAccount> selAcc = mAdapter.getSelectAccountList();
        if (selAcc.size() == 0){
            return;
        }else{
            showAlertInMain("Delete account ?",
                    "Click delete button will delete account " + selAcc.get(0).mAccountName,
                    "Cancel", "Delete");

        }

    }

    public void onAddButtonClick(View v){

        ArrayList<Pair<String, String>> coinTokenArr = new ArrayList<>();
        for(Pair<String, String> coinToken : getAvailableCoinTokenArr()){
            boolean hasAcc = false;
            for(CoinTokenAccount account : mTokenAccountArray){
                if (coinToken.first.compareTo(account.mCoinType)==0 && coinToken.second.compareTo(account.mToken)==0){
                    hasAcc = true;
                    break;
                }
            }

            if (!hasAcc){
                coinTokenArr.add(coinToken);
            }
        }

        if (coinTokenArr.size()==0){
            return;
        }

        mCoinTokenListDlg.show(mContext, coinTokenArr);

    }

    public void onAddAccountButtonClick(View v){

        ArrayList<Pair<String, String>> coinTokenArr = mCoinTokenListDlg.getSelectAccountList();
        if (coinTokenArr.size()==0){
            showMessageInMain("Please select a coin & token pair");
            return;
        }

        mCoinTokenListDlg.dismiss();

        showProgress("Adding...");
        new Thread(new Runnable() {
            @Override
            public void run() {

                for (Pair<String, String> item : coinTokenArr) {
                    Pair<Integer, String> ret = mAccountManager.coinAccountOperation(item.first,  "", "", "Add");
                    if (ret.first == SecuXServerRequestHandler.SecuXRequestOK) {
                        Log.i(TAG, "Create account done");
                    }else{
                        showMessageInMain("Create account " + item.second + " failed!");
                    }
                }

                loadAccounts();
                hideProgressInMain();
            }
        }).start();
    }

    private ArrayList<Pair<String, String>> getAvailableCoinTokenArr(){
        ArrayList<Pair<String, String>> availableCoinTokenArr = new ArrayList<>(2);
        //availableCoinTokenArr.add(new Pair<>("LBR", "LBR"));
        availableCoinTokenArr.add(new Pair<>("LBR", "Coin1"));
        availableCoinTokenArr.add(new Pair<>("CELO", "cUSD"));
        //availableCoinTokenArr.add(new Pair<>("DCT", "NTD"));
        return availableCoinTokenArr;
    }

    private boolean showAddAccountButton(){
        ArrayList<Pair<String, String>> coinTokenArr = new ArrayList<>();

        for(Pair<String, String> coinToken : getAvailableCoinTokenArr()){
            boolean hasAcc = false;
            for(CoinTokenAccount account : mTokenAccountArray){
                if (coinToken.first.compareTo(account.mCoinType)==0 && coinToken.second.compareTo(account.mToken)==0){
                    hasAcc = true;
                    break;
                }
            }

            if (!hasAcc){
                coinTokenArr.add(coinToken);
            }
        }

        if (coinTokenArr.size()==0){
            return false;
        }

        return true;
    }

    private void loadAccounts(){
        Pair<Integer, String> ret = mAccountManager.getCoinAccountList(Setting.getInstance().mAccount);
        if (ret.first!= SecuXServerRequestHandler.SecuXRequestOK){

            showMessageInMain("Get coin token account list failed!");
            return;
        }

        mTokenAccountArray = AccountUtil.getCoinTokenAccounts();
        for(CoinTokenAccount account : mTokenAccountArray){
            mAccountManager.getAccountBalance(Setting.getInstance().mAccount, account.mCoinType, account.mToken);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAdapter == null) {
                    final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView_acclist_accounts);
                    mAdapter = new CoinAccountListAdapter(mContext, mTokenAccountArray, mItemClickListener);
                    recyclerView.setAdapter(mAdapter);
                }else{
                    mAdapter.updateAccountList(mTokenAccountArray);
                    mAdapter.notifyDataSetChanged();
                }

                if (showAddAccountButton()) {
                    mButtonAdd.setVisibility(View.VISIBLE);
                }else{
                    mButtonAdd.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_main_accounts:
                    return true;

                case R.id.navigation_main_payment:
                    Intent newIntent = new Intent(mContext, PaymentMainActivity.class);
                    startActivity(newIntent);
                    return true;

                case R.id.navigation_main_userinfo:
                    Intent userInfoIntent = new Intent(mContext, UserInfoActivity.class);
                    startActivity(userInfoIntent);
                    return true;
            }
            return false;
        }

    };

    @Override
    public void onOKButtonClick(View v){
        super.onOKButtonClick(v);

        showProgress("Deleting...");
        ArrayList<CoinTokenAccount> selAcc = mAdapter.getSelectAccountList();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (CoinTokenAccount acc : selAcc){
                    mAccountManager.coinAccountOperation(acc.mCoinType, acc.mAccountName, "Unbind the account", "Unbind");
                }
                loadAccounts();
                hideProgressInMain();
            }
        }).start();
    }
}
