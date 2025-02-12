package com.secuxtech.mysecuxpay.Activity;


import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import android.content.res.AssetFileDescriptor;


import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.util.Pair;

import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.secuxtech.mysecuxpay.Adapter.CoinAccountListAdapter;
import com.secuxtech.mysecuxpay.BuildConfig;
import com.secuxtech.mysecuxpay.Interface.AdapterItemClickListener;
import com.secuxtech.mysecuxpay.Model.CoinTokenAccount;
import com.secuxtech.mysecuxpay.Model.Setting;
import com.secuxtech.mysecuxpay.R;
import com.secuxtech.mysecuxpay.Utility.AccountUtil;
import com.secuxtech.mysecuxpay.Utility.CommonProgressDialog;

import com.secuxtech.mysecuxpay.biometric.BiometricCallback;
import com.secuxtech.mysecuxpay.biometric.BiometricManager;

import com.secuxtech.paymentkit.SecuXAccountManager;
import com.secuxtech.paymentkit.SecuXPaymentHistory;
import com.secuxtech.paymentkit.SecuXPaymentManager;
import com.secuxtech.paymentkit.SecuXPaymentManagerCallback;
import com.secuxtech.paymentkit.SecuXServerRequestHandler;
import com.secuxtech.paymentkit.SecuXStoreInfo;


import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;

import io.sentry.core.Sentry;


public class PaymentDetailsActivity extends BaseActivity {

    public static final String PAYMENT_RESULT = "com.secux.MySecuXPay.PAYMENTRESULT";
    public static final String PAYMENT_ERROR = "com.secux.MySecuXPay.PAYMENTERROR";

    public static final String PAYMENT_STORENAME = "com.secux.MySecuXPay.STORENAME";
    public static final String PAYMENT_AMOUNT = "com.secux.MySecuXPay.AMOUNT";
    public static final String PAYMENT_DEVID = "com.secux.MySecuXPay.DEVID";
    public static final String PAYMENT_DEVIDHASH = "com.secux.MySecuXPay.DEVIDHASH";

    public static final String PAYMENT_DATE = "com.secux.MySecuXPay.DATE";
    public static final String PAYMENT_NONCE = "com.secux.MySecuXPay.NONCE";

    public static final int REQUEST_PWD_PROMPT = 1;

    public static SecuXStoreInfo mStoreInfo = null;
    public static ArrayList<CoinTokenAccount> mCoinTokenAccountList = null;

    private CoinTokenAccount mCoinTokenAccount = null;
    private Context mContext = this;
    private ProgressBar mProgressBar;

    private SecuXAccountManager mAccountManager = new SecuXAccountManager();
    private SecuXPaymentManager mPaymentManager = new SecuXPaymentManager();

    private String mPaymentInfo = ""; //"{\"amount\":\"11\", \"coinType\":\"DCT\", \"deviceID\":\"4ab10000726b\"}";
    private String mAmount = "";
    private String mType = "";
    private String mToken = "";

    private String mDevID = "";
    private String mDevIDhash = "";
    private String mNonce = "";


    private boolean mAuthenicationScreenShow = false;

    private Dialog mAccountSelDialog;
    private boolean mShowAccountSel = false;

    private EditText mEditTextAmount;
    private Button mButtonPay;
    private ImageView mImageViewNext;

    private BiometricManager mBioManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_details);

        checkBLESetting();

        Setting.getInstance().mLastPaymentHis = null;

        Intent intent = getIntent();
        mPaymentInfo = intent.getStringExtra(PaymentMainActivity.PAYMENT_INFO);
        mAmount = intent.getStringExtra(PAYMENT_AMOUNT);
        mDevID = intent.getStringExtra(PAYMENT_DEVID);
        mDevIDhash = intent.getStringExtra(PAYMENT_DEVIDHASH);
        mNonce = intent.getStringExtra(PAYMENT_NONCE);

        mCoinTokenAccount = mCoinTokenAccountList.get(0);
        mType = mCoinTokenAccount.mCoinType;
        mToken = mCoinTokenAccount.mToken;
        mShowAccountSel = mCoinTokenAccountList.size() > 1;

        ImageView imageviewLogo = findViewById(R.id.imageView_account_coinlogo);
        imageviewLogo.setImageResource(AccountUtil.getCoinLogo(mType));

        TextView textviewName = findViewById(R.id.textView_account_name);
        textviewName.setText(Setting.getInstance().mAccount.getCoinAccount(mType).mAccountName);

        TextView textviewBalance = findViewById(R.id.textView_account_balance);
        textviewBalance.setText(String.format("%.2f", mCoinTokenAccount.mBalance.mFormattedBalance) + " " + mToken);

        mButtonPay = findViewById(R.id.button_pay);

        mImageViewNext = findViewById(R.id.imageView_next);

        mEditTextAmount = findViewById(R.id.editText_paymentinput_amount);
        if (Double.valueOf(mAmount) > 0.0){
            mEditTextAmount.setText(mAmount);
            mEditTextAmount.setFocusable(false);
            mButtonPay.setEnabled(true);
            mImageViewNext.setVisibility(View.INVISIBLE);

        }else{
            mEditTextAmount.setFocusable(true);
            textviewName.requestFocus();
            mButtonPay.setEnabled(false);

            if (mCoinTokenAccountList.size() > 1)
                mImageViewNext.setVisibility(View.VISIBLE);

        }

        ImageView payinputLogo = findViewById(R.id.imageView_paymentinput_coinlogo);
        payinputLogo.setImageResource(AccountUtil.getCoinLogo(mType));

        TextView textviewPaymentType = findViewById(R.id.textView_paymentinput_coinname);
        textviewPaymentType.setText(mToken);

        TextView textviewStoreName = findViewById(R.id.textView_storename);
        textviewStoreName.setText(mStoreInfo.mName);

        ImageView imgviewStoreLogo = findViewById(R.id.imageView_storelogo);
        imgviewStoreLogo.setVisibility(View.VISIBLE);

        imgviewStoreLogo.setImageBitmap(mStoreInfo.mLogo);


        //mProgressBar = (ProgressBar) findViewById(R.id.progressBar_load_storeinfo);
        //mProgressBar.setVisibility(View.VISIBLE);

        EditText edittext = findViewById(R.id.editText_paymentinput_amount);
        edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (!hasFocus) {
                    hideKeyboard(v);

                    String strAmount = mEditTextAmount.getText().toString();
                    if (strAmount.length() == 0){

                        mButtonPay.setEnabled(false);
                        return;
                    }
                    Double payAmount = Double.valueOf(strAmount);
                    if (payAmount<=0){

                        mButtonPay.setEnabled(false);
                        return;
                    }

                    mButtonPay.setEnabled(true);
                }
            }
        });

        edittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE){
                    String strAmount = mEditTextAmount.getText().toString();
                    if (strAmount.length() == 0){

                        mButtonPay.setEnabled(false);
                        return false;
                    }
                    Double payAmount = Double.valueOf(strAmount);
                    if (payAmount<=0){

                        mButtonPay.setEnabled(false);
                        return false;
                    }

                    mButtonPay.setEnabled(true);
                }

                return false;
            }
        });


        CardView cardViewAccount = findViewById(R.id.cardView_account);
        cardViewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAccount(v);
            }
        });

        mPaymentManager.setSecuXPaymentManagerCallback(mPaymentMgrCallback);


    }


    public void onPayButtonClick(View v){

        if (!this.checkWifi()){
            showAlert("No network!", "Please check the phone's network setting");
            return;
        }

        if (!this.checkBLESetting()){
            return;
        }




        String strAmount = mEditTextAmount.getText().toString();
        if (strAmount.length() == 0){

            showAlert("No payment amount!", "Payment abort!");
            return;
        }
        Double payAmount = Double.valueOf(strAmount);
        if (payAmount<=0 || payAmount > mCoinTokenAccount.mBalance.mFormattedBalance.doubleValue()){

            showAlert("Invalid payment amount!", "Payment abort!");
            return;
        }

        mAmount = strAmount;

        if (BuildConfig.DEBUG && Setting.getInstance().mTestModel){

            SecuXPaymentHistory payhistory = new SecuXPaymentHistory();
            Pair<Integer, String> hisret = mPaymentManager.getPaymentHistory(mToken, "b2a908614bb8484aa8864c6ac0ba709b", payhistory);
            if (hisret.first == SecuXServerRequestHandler.SecuXRequestOK){
                Setting.getInstance().mLastPaymentHis = payhistory;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    SimpleDateFormat simpleDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
                    Date date = Calendar.getInstance().getTime();
                    String dateStr = simpleDateFormat.format(date);

                    String amountStr = mAmount.toString() + " " + mToken;

                    Intent newIntent = new Intent(mContext, PaymentResultActivity.class);
                    newIntent.putExtra(PAYMENT_RESULT, true);
                    newIntent.putExtra(PAYMENT_STORENAME, mStoreInfo.mName);
                    newIntent.putExtra(PAYMENT_AMOUNT, amountStr);
                    newIntent.putExtra(PAYMENT_DATE, dateStr);

                    startActivity(newIntent);
                }
            });

            return;
        }

        /*
        mMonitorPaymentTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (CommonProgressDialog.isProgressVisible()){
                    mPaymentManager.cancelPayment();
                    CommonProgressDialog.dismiss();

                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        v.vibrate(500);
                    }

                    MediaPlayer mediaPlayer = new MediaPlayer();
                    AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.payfailed);
                    try {
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        mediaPlayer.prepare();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                    mediaPlayer.start();


                    String amountStr = mAmount + " " + mType;

                    SimpleDateFormat simpleDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
                    Date date = Calendar.getInstance().getTime();
                    String dateStr = simpleDateFormat.format(date);

                    Intent newIntent = new Intent(mContext, PaymentResultActivity.class);
                    newIntent.putExtra(PAYMENT_RESULT, false);
                    newIntent.putExtra(PAYMENT_STORENAME, mStoreName);
                    newIntent.putExtra(PAYMENT_AMOUNT, amountStr);
                    newIntent.putExtra(PAYMENT_DATE, dateStr);
                    startActivity(newIntent);
                }
            }
        }, 10000);
        */

        /*
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled :)

            showAlert("Please turn on Bluetooth!", "Payment abort!");
            return;
        }

         */

        if (Setting.getInstance().mEnableBioVerifyBeforePayment) {
            try {
                mBioManager = new BiometricManager.BiometricBuilder(this)
                        .setTitle("Pay to " + mStoreInfo.mName)
                        .setSubtitle("SecuX EvPay")
                        .setDescription("Allow payment with your biometric ID")
                        .setNegativeButtonText("Cancel")
                        .build();

                mBioManager.authenticate(mBiometricCallback);
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
                Sentry.captureException(e);
                doPayment();
            }
        }else{
            doPayment();
        }
    }

    private void doPayment(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CommonProgressDialog.showProgressDialog(mContext);
            }
        });


        try {
            JSONObject payInfoJson = new JSONObject();
            payInfoJson.put("amount", mAmount);
            payInfoJson.put("coinType", mType);
            payInfoJson.put("token", mToken);
            payInfoJson.put("deviceID", mDevID);
            mPaymentInfo = payInfoJson.toString();

            if (mNonce.length() == 0) {
                mPaymentManager.doPayment(mContext, Setting.getInstance().mAccount, mStoreInfo.mInfo, mPaymentInfo);
            }else {
                mPaymentManager.doPayment(mNonce, mContext, Setting.getInstance().mAccount, mStoreInfo.mInfo, mPaymentInfo);
            }

        }catch (Exception e){

            showAlertInMain("Generate payment data failed!", "Payment abort!");
            return;
        }
    }

    private BiometricCallback mBiometricCallback = new BiometricCallback() {
        @Override
        public void onSdkVersionNotSupported() {
            Log.i(TAG, "onSdkVersionNotSupported");
            showAuthenticationScreen();
        }

        @Override
        public void onBiometricAuthenticationNotSupported() {
            Log.i(TAG, "onBiometricAuthenticationNotSupported");
            showAuthenticationScreen();
        }

        @Override
        public void onBiometricAuthenticationNotAvailable() {
            Log.i(TAG, "onBiometricAuthenticationNotAvailable");
            showAuthenticationScreen();
        }

        @Override
        public void onBiometricAuthenticationPermissionNotGranted() {
            Log.i(TAG, "onBiometricAuthenticationPermissionNotGranted");
            showAuthenticationScreen();
        }

        @Override
        public void onBiometricAuthenticationInternalError(String error) {
            Log.i(TAG, "onBiometricAuthenticationInternalError");
            showAuthenticationScreen();
        }

        @Override
        public void onAuthenticationFailed() {
            Log.i(TAG, "onAuthenticationFailed");

        }

        @Override
        public void onAuthenticationCancelled() {

            Log.i(TAG, "onAuthenticationCancelled");
        }

        @Override
        public void onAuthenticationSuccessful() {

            Log.i(TAG, "onAuthenticationSuccessful");
            doPayment();
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            Log.i(TAG, "onAuthenticationHelp");
            showAuthenticationScreen();
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            Log.i(TAG, "onAuthenticationError " + errorCode + " " + errString);

        }
    };

    private void showAuthenticationScreen(){
        if (mAuthenicationScreenShow)
            return;

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (!km.isDeviceSecure()){
            doPayment();
            return;
        }

        Log.i("", "showAuthenticationScreen");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

            // get the intent to prompt the user
            Intent intent = km.createConfirmDeviceCredentialIntent("SecuX EvPay", "Enter your password to pay");
            // launch the intent
            if (intent != null) {
                startActivityForResult(intent, REQUEST_PWD_PROMPT);
                mAuthenicationScreenShow = true;
            }

        }else{


            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Pay to " + mStoreInfo.mName)
                    .setSubtitle("SecuX EvPay")
                    .setDescription("Allow payment with your biometric ID")
                    .setDeviceCredentialAllowed(true)
                    .build();

            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(PaymentDetailsActivity.this,
                    executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode,
                                                  @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);

                }

                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(200);
                            }catch (Exception e){

                            }
                            doPayment();
                        }
                    }).start();

                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();

                }
            });

            biometricPrompt.authenticate(promptInfo);



        }

    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        super.onActivityResult(resultCode, resultCode, data);
        // see if this is being called from our password request..?
        if (requestCode == REQUEST_PWD_PROMPT) {
            // ..it is. Did the user get the password right?
            if (resultCode == RESULT_OK) {
                // they got it right
                doPayment();
            } else {
                // they got it wrong/cancelled
            }
        }
        mAuthenicationScreenShow = false;
    }

    public void onClickAccount(View v){
        if (!mShowAccountSel){
            return;
        }
        Log.i(TAG, "click the account");
        showDialog(this);
    }

    public void showDialog(Activity activity){

        mAccountSelDialog = new Dialog(activity);
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mAccountSelDialog.setCancelable(true);
        mAccountSelDialog.setContentView(R.layout.dialog_account_list_selection_layout);

        AdapterItemClickListener mItemClickListener = new AdapterItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mCoinTokenAccount = mCoinTokenAccountList.get(position);
                Log.i(TAG, mCoinTokenAccount.mAccountName);

                mType = mCoinTokenAccount.mCoinType;
                mToken = mCoinTokenAccount.mToken;

                ImageView imageviewLogo = findViewById(R.id.imageView_account_coinlogo);
                imageviewLogo.setImageResource(AccountUtil.getCoinLogo(mCoinTokenAccount.mCoinType));

                TextView textviewName = findViewById(R.id.textView_account_name);
                textviewName.setText(mCoinTokenAccount.mAccountName);

                TextView textviewBalance = findViewById(R.id.textView_account_balance);
                textviewBalance.setText(String.format("%.2f", mCoinTokenAccount.mBalance.mFormattedBalance) + " " + mToken);

                //TextView textviewUsdbalance = findViewById(R.id.textView_account_usdbalance);
                //textviewUsdbalance.setText(String.format("$ %.2f", mTokenBalance.mUSDBalance));

                ImageView payinputLogo = findViewById(R.id.imageView_paymentinput_coinlogo);
                payinputLogo.setImageResource(AccountUtil.getCoinLogo(mCoinTokenAccount.mCoinType));

                TextView textviewPaymentType = findViewById(R.id.textView_paymentinput_coinname);
                textviewPaymentType.setText(mCoinTokenAccount.mCoinType);

                mAccountSelDialog.dismiss();

            }
        };

        RecyclerView recyclerView = mAccountSelDialog.findViewById(R.id.recyclerView_accountsel_dialog);
        CoinAccountListAdapter adapterRe = new CoinAccountListAdapter(PaymentDetailsActivity.this, mCoinTokenAccountList, mItemClickListener);
        recyclerView.setAdapter(adapterRe);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        mAccountSelDialog.show();

    }

    //Callback for SecuXPaymentManager
    private SecuXPaymentManagerCallback mPaymentMgrCallback = new SecuXPaymentManagerCallback() {

        //Called when payment is completed. Returns payment result and error message.
        @Override
        public void paymentDone(final boolean ret, final String transactionCode, final String errorMsg) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (ret){

                        mAccountManager.getAccountBalance(Setting.getInstance().mAccount, mType, mToken);
                        SecuXPaymentHistory payhistory = new SecuXPaymentHistory();

                        Pair<Integer, String> hisret = mPaymentManager.getPaymentHistory(mToken, transactionCode, payhistory);
                        if (hisret.first == SecuXServerRequestHandler.SecuXRequestOK) {
                            Setting.getInstance().mLastPaymentHis = payhistory;
                        } else {
                            Log.e(TAG, "Get transaction history from " + transactionCode + " failed!");
                        }
                    }

                    //mMonitorPaymentTimer.cancel();
                    hideProgressInMain();

                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        v.vibrate(500);
                    }

                    MediaPlayer mediaPlayer = new MediaPlayer();
                    AssetFileDescriptor afd;

                    SimpleDateFormat simpleDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
                    Date date = Calendar.getInstance().getTime();
                    String dateStr = simpleDateFormat.format(date);
                    if (ret){
                        //Toast toast = Toast.makeText(mContext, "Payment successful!", Toast.LENGTH_LONG);
                        //toast.setGravity(Gravity.CENTER,0,0);
                        //toast.show();

                        //Double usdAmount = Wallet.getInstance().getUSDValue(Double.valueOf(mAmount), mAccount.mCoinType);

                        //PaymentHistoryModel payment = new PaymentHistoryModel(mAccount, mStoreName, dateStr, String.format("%.2f", usdAmount), mAmount);
                        //Wallet.getInstance().addPaymentHistoryItem(payment);


                        afd = getResources().openRawResourceFd(R.raw.paysuccess);


                    }else{

                        //Toast toast = Toast.makeText(mContext, "Payment failed! Error: " + message, Toast.LENGTH_LONG);
                        //toast.setGravity(Gravity.CENTER,0,0);
                        //toast.show();


                        afd = getResources().openRawResourceFd(R.raw.payfailed);
                    }

                    try {
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        mediaPlayer.prepare();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                    mediaPlayer.start();

                    String amountStr = mAmount.toString() + " " + mToken;
                    String message = errorMsg;
                    if (message.contains("Scan timeout")){
                        message = "No payment device!";
                    }

                    final String showMsg = message;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent newIntent = new Intent(mContext, PaymentResultActivity.class);
                            newIntent.putExtra(PAYMENT_RESULT, ret);
                            newIntent.putExtra(PAYMENT_ERROR, showMsg);
                            newIntent.putExtra(PAYMENT_STORENAME, mStoreInfo.mName);
                            newIntent.putExtra(PAYMENT_AMOUNT, amountStr);
                            newIntent.putExtra(PAYMENT_DATE, dateStr);

                            startActivity(newIntent);

                            finish();
                        }
                    });

                }
            }).start();

        }

        //Called when payment status is changed. Payment status are: "Device connecting...", "DCT transferring..." and "Device verifying..."
        @Override
        public void updatePaymentStatus(final String status){
            Log.i("secux-paymentkit-exp", "Update payment status:" + SystemClock.uptimeMillis() + " " + status);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CommonProgressDialog.setProgressTip(status);
                }
            });
        }


        @Override
        public void userAccountUnauthorized(){
            showMessageInMain("User account authorization timeout! Please login again");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent newIntent = new Intent(mContext, LoginActivity.class);
                    startActivity(newIntent);
                }
            });
        }

    };

}
