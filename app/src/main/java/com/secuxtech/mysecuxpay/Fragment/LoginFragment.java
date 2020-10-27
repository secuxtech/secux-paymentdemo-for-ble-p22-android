package com.secuxtech.mysecuxpay.Fragment;



import android.app.KeyguardManager;
import android.content.Context;

import android.content.Intent;

import android.os.Build;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;


import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.secuxtech.mysecuxpay.Model.Setting;
import com.secuxtech.mysecuxpay.R;

import com.secuxtech.mysecuxpay.biometric.BiometricCallback;
import com.secuxtech.mysecuxpay.biometric.BiometricManager;
import com.secuxtech.mysecuxpay.biometric.BiometricUtils;
import com.secuxtech.paymentkit.SecuXUserAccount;


import java.util.concurrent.Executor;

import static android.app.Activity.RESULT_OK;
import static androidx.constraintlayout.widget.Constraints.TAG;
import static com.secuxtech.mysecuxpay.Activity.PaymentDetailsActivity.REQUEST_PWD_PROMPT;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends BaseFragment {


    private EditText mEdittextEmail;
    private EditText mEdittextPwd;
    private TextView mTextViewInvalidEmail;
    private TextView mTextViewInvalidPwd;
    private Button mButtonLogin;

    private boolean mAuthenicationScreenShow = false;

    private BiometricManager mBioManager = null;
    private int mAuthenticationRetryCount = 0;

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mEdittextEmail = view.findViewById(R.id.editText_lgoin_email);
        mEdittextEmail.setOnFocusChangeListener(mViewFocusChangeListener);
        mEdittextEmail.setOnEditorActionListener(mTextviewEditorListener);
        //mEdittextEmail.addTextChangedListener(mTextWatcher);

        mEdittextPwd = view.findViewById(R.id.editText_lgoin_password);
        mEdittextPwd.setOnFocusChangeListener(mViewFocusChangeListener);
        mEdittextPwd.setOnEditorActionListener(mTextviewEditorListener);
        //mEdittextPwd.addTextChangedListener(mTextWatcher);

        mTextViewInvalidEmail = view.findViewById(R.id.textView_login_invalid_email);
        mTextViewInvalidPwd = view.findViewById(R.id.textView_login_invalid_password);
        mButtonLogin = view.findViewById(R.id.button_login);

        Button loginBtn = view.findViewById(R.id.button_login);
        loginBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginButtonClick(v);
            }
        });

        TextView idloginBtn = view.findViewById(R.id.textView_login_bioid);
        idloginBtn.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUseTouchIDFaceIDLoginClick(v);
            }
        });

        //Setting sss = Setting.getInstance();
        if (Setting.getInstance().mAccount == null) {
            Setting.getInstance().loadSettings(getActivity());
        }

        TextView bioLoginTextView = view.findViewById(R.id.textView_login_bioid);
        View underlineView = view.findViewById(R.id.view_login_underline);

        if (!Setting.getInstance().mUserLogout && Setting.getInstance().mUserAccountName!="" && Setting.getInstance().mUserAccountPwd!=""){

            bioLoginTextView.setVisibility(View.VISIBLE);
            underlineView.setVisibility(View.VISIBLE);

            if(BiometricUtils.isSdkVersionSupported() &&
               BiometricUtils.isPermissionGranted(getActivity()) &&
               BiometricUtils.isHardwareSupported(getActivity()) &&
               BiometricUtils.isFingerprintAvailable(getActivity())){

                onUseTouchIDFaceIDLoginClick(null);
            }
        }else{

            bioLoginTextView.setVisibility(View.INVISIBLE);
            underlineView.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    private View.OnFocusChangeListener mViewFocusChangeListener = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View v, boolean hasFocus) {

            if (!hasFocus) {
                hideKeyboard(v);
                checkInput(v);
            }
        }
    };


    private TextView.OnEditorActionListener mTextviewEditorListener = new EditText.OnEditorActionListener(){
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            Log.i(TAG, "onEditorAction " + String.valueOf(actionId));
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null &&
                            event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    // the user is done typing.
                    Log.i(TAG, "Edit done");


                    if (checkInput(v) && v==mEdittextPwd){
                        onLoginButtonClick(v);
                    }
                    return true; // consume.
                }
            }
            return false; // pass on to other listeners.
        }
    };

    private boolean checkInput(View v){
        if (v == mEdittextPwd){
            String pwd = mEdittextPwd.getText().toString();
            if (pwd.length()==0){
                mTextViewInvalidPwd.setVisibility(View.VISIBLE);
            }else{
                mTextViewInvalidPwd.setVisibility(View.INVISIBLE);
                return true;
            }
        }else if (v==mEdittextEmail){
            String email = mEdittextEmail.getText().toString();
            if (email.length()==0 || !email.contains("@") || !email.contains(".") ||
                    email.indexOf('@')==email.length()-1 || email.indexOf('.')==email.length()-1 ||
                    email.indexOf('@')==0 || email.indexOf('.')==0){
                mTextViewInvalidEmail.setVisibility(View.VISIBLE);
            }else{
                mTextViewInvalidEmail.setVisibility(View.INVISIBLE);
                return true;
            }
        }
        return false;
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
            mBioManager.cancelAuthentication();
            Log.i(TAG, "onAuthenticationCancelled");
        }

        @Override
        public void onAuthenticationSuccessful() {
            Log.i(TAG, "onAuthenticationSuccessful");
            mEdittextEmail.setText(Setting.getInstance().mUserAccountName);
            mEdittextPwd.setText(Setting.getInstance().mUserAccountPwd);
            onLoginButtonClick(mButtonLogin);
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            Log.i(TAG, "onAuthenticationHelp");
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            Log.i(TAG, "onAuthenticationError " + errorCode + " " + errString);
        }


    };

    private void loginAuthenication(){
        if (Setting.getInstance().mUserAccountName!="" && Setting.getInstance().mUserAccountPwd!=""){

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBioManager = new BiometricManager.BiometricBuilder(LoginFragment.this)
                            .setTitle("Login")
                            .setSubtitle("SecuX EvPay")
                            .setDescription("Auto login with your biometric ID")
                            .setNegativeButtonText("Cancel")
                            .build();

                    mBioManager.authenticate(mBiometricCallback);

                }
            });

        }
    }

    private void showAuthenticationScreen(){
        if (mAuthenicationScreenShow)
            return;

        KeyguardManager km = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        if (!km.isDeviceSecure()){
            onLoginButtonClick(mButtonLogin);
            return;
        }

        Log.i("", "showAuthenticationScreen");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

            // get the intent to prompt the user
            Intent intent = km.createConfirmDeviceCredentialIntent("SecuX EvPay", "Enter your password to login");
            // launch the intent
            if (intent != null) {
                startActivityForResult(intent, REQUEST_PWD_PROMPT);
                mAuthenicationScreenShow = true;
            }

        }else{

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Auto login" )
                    .setSubtitle("SecuX EvPay")
                    .setDescription("Allow login with your biometric ID")
                    .setDeviceCredentialAllowed(true)
                    .build();

            Executor executor = ContextCompat.getMainExecutor(getContext());
            BiometricPrompt biometricPrompt = new BiometricPrompt(LoginFragment.this,
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

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mEdittextEmail.setText(Setting.getInstance().mUserAccountName);
                                    mEdittextPwd.setText(Setting.getInstance().mUserAccountPwd);
                                    onLoginButtonClick(mButtonLogin);
                                }
                            });

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
        /*
        if (mAuthenicationScreenShow){
            return;
        }

        if (getActivity() == null){
            return;
        }

        KeyguardManager km = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        // get the intent to prompt the user
        Intent intent = km.createConfirmDeviceCredentialIntent("SecuX EvPay", "Enter your password to login");
        // launch the intent
        if (intent!=null) {
            startActivityForResult(intent, REQUEST_PWD_PROMPT);
            mAuthenicationScreenShow = true;
        }

         */
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data){
        super.onActivityResult(resultCode, resultCode, data);
        // see if this is being called from our password request..?
        if (requestCode == REQUEST_PWD_PROMPT) {
            // ..it is. Did the user get the password right?
            if (resultCode == RESULT_OK) {
                // they got it right
                mEdittextEmail.setText(Setting.getInstance().mUserAccountName);
                mEdittextPwd.setText(Setting.getInstance().mUserAccountPwd);
                onLoginButtonClick(mButtonLogin);
            } else {
                // they got it wrong/cancelled
            }
        }
        mAuthenicationScreenShow = false;
    }

    public void onUseTouchIDFaceIDLoginClick(View v){
        mAuthenticationRetryCount = 0;
        loginAuthenication();
    }


    public void onLoginButtonClick(View v)
    {
        //Intent newIntent = new Intent(mContext, MainActivity.class);
        //startActivity(newIntent);

        hideKeyboard(v);

        if (!checkWifi()){
            showAlert("No network!", "Please check the phone's network setting");
            return;
        }

        checkInput(mEdittextEmail);
        checkInput(mEdittextPwd);

        if (mTextViewInvalidEmail.getVisibility()==View.VISIBLE || mTextViewInvalidPwd.getVisibility()==View.VISIBLE){
            //Toast toast = Toast.makeText(getActivity(), "Invalid email account or password!", Toast.LENGTH_LONG);
            //toast.setGravity(Gravity.CENTER,0,0);
            //toast.show();

            showAlert("Login failed!", "Invalid email account or password!");

            return;
        }

        String email = mEdittextEmail.getText().toString();
        String pwd = mEdittextPwd.getText().toString();

        //Setting.getInstance().mAccount = new SecuXUserAccount("maochuntest6@secuxtech.com", "0975123456", "12345678");
        final SecuXUserAccount account = new SecuXUserAccount(email, pwd);
        login(account);
    }

}
