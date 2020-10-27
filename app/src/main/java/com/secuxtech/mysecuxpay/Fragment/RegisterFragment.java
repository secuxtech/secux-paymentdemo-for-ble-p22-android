package com.secuxtech.mysecuxpay.Fragment;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.secuxtech.mysecuxpay.Adapter.SupportedCoinTokeListAdapter;

import com.secuxtech.mysecuxpay.Model.Setting;
import com.secuxtech.mysecuxpay.R;
import com.secuxtech.mysecuxpay.Utility.AccountUtil;
import com.secuxtech.mysecuxpay.Utility.CommonProgressDialog;
import com.secuxtech.mysecuxpay.Utility.ExpandCollapseAnimation;
import com.secuxtech.mysecuxpay.Utility.SecuXUtility;
import com.secuxtech.paymentkit.SecuXAccountManager;
import com.secuxtech.paymentkit.SecuXServerRequestHandler;
import com.secuxtech.paymentkit.SecuXUserAccount;

import static androidx.constraintlayout.widget.Constraints.TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends BaseFragment {

    private SecuXAccountManager mAccountManager = new SecuXAccountManager();

    private EditText mEdittextEmail;
    private EditText mEdittextPwd;
    private EditText mEdittextConfirmPwd;
    private EditText mEdittextPhone;
    private TextView mTextViewInvalidEmail;
    private TextView mTextViewInvalidPwd;
    private TextView mTextViewInvalidConfirmPwd;
    private TextView mTextViewInvlidePhone;

    private ListView mListViewCoinToken;

    TextView  mTextViewSelTokenItem;
    ImageView mImageViewSelCoinLogo;

    private boolean mShowCoinTokenSelList = false;
    private Pair<String, String> mSelCoinTokenItem = null;

    public RegisterFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        mEdittextEmail = view.findViewById(R.id.editText_register_email);
        mEdittextEmail.setOnFocusChangeListener(mViewFocusChangeListener);
        mEdittextEmail.setOnEditorActionListener(mTextviewEditorListener);

        mEdittextPhone = view.findViewById(R.id.editText_register_phone);
        mEdittextPhone.setOnFocusChangeListener(mViewFocusChangeListener);
        mEdittextPhone.setOnEditorActionListener(mTextviewEditorListener);

        mEdittextPwd = view.findViewById(R.id.editText_register_password);
        mEdittextPwd.setOnFocusChangeListener(mViewFocusChangeListener);
        mEdittextPwd.setOnEditorActionListener(mTextviewEditorListener);

        mEdittextConfirmPwd = view.findViewById(R.id.editText_register_confirmpassword);
        mEdittextConfirmPwd.setOnFocusChangeListener(mViewFocusChangeListener);
        mEdittextConfirmPwd.setOnEditorActionListener(mTextviewEditorListener);

        mTextViewInvalidEmail = view.findViewById(R.id.textView_register_invalid_email);
        mTextViewInvlidePhone = view.findViewById(R.id.textView_register_invalid_phone);
        mTextViewInvalidPwd = view.findViewById(R.id.textView_register_invalid_password);
        mTextViewInvalidConfirmPwd = view.findViewById(R.id.textView_register_invalid_confirmpassword);

        mImageViewSelCoinLogo = view.findViewById(R.id.imageView_cointokensel_coinlogo);
        mTextViewSelTokenItem = view.findViewById(R.id.textView_cointokensel_txt);

        view.setOnTouchListener(mViewTouchListener);

        mListViewCoinToken = view.findViewById(R.id.listview_supported_cointoken);

        mListViewCoinToken.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG,"click item " + position);

                Pair<String, String> item = Setting.getInstance().mCoinTokenArray.get(position);

                mTextViewSelTokenItem.setText(item.second);
                mImageViewSelCoinLogo.setImageResource(AccountUtil.getCoinLogo(item.first));
                mSelCoinTokenItem = item;

                toggleCoinTokenListView();
            }
        });
        mListViewCoinToken.getLayoutParams().height = 0;
        mListViewCoinToken.setVisibility(View.INVISIBLE);

        Button loginBtn = view.findViewById(R.id.button_register);
        loginBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRegisterButtonClick(v);
            }
        });

        return view;

    }

    private View.OnFocusChangeListener mViewFocusChangeListener = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View v, boolean hasFocus) {

            if (!hasFocus) {
                hideKeyboard(v);
                checkInput(v);
            }

            if (mShowCoinTokenSelList){
                mListViewCoinToken.setVisibility(View.INVISIBLE);
                toggleCoinTokenListView();
            }

        }
    };

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener(){

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (mShowCoinTokenSelList) {
                mListViewCoinToken.setVisibility(View.INVISIBLE);
                toggleCoinTokenListView();
            }


            return true;
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
                    if (checkInput(v) && v == mEdittextConfirmPwd){
                        onRegisterButtonClick(v);
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
            if (pwd.length()<6){
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
        }else if (v==mEdittextPhone){
            String phone = mEdittextPhone.getText().toString();
            if (phone.length()==0){
                mTextViewInvlidePhone.setVisibility(View.VISIBLE);
            }else{
                mTextViewInvlidePhone.setVisibility(View.INVISIBLE);
                return true;
            }
        }else if (v==mEdittextConfirmPwd){
            String pwd = mEdittextPwd.getText().toString();
            String confirmPwd = mEdittextConfirmPwd.getText().toString();
            if (confirmPwd.length()==0 || confirmPwd.compareTo(pwd)!=0){
                mTextViewInvalidConfirmPwd.setVisibility(View.VISIBLE);
            }else{
                mTextViewInvalidConfirmPwd.setVisibility(View.INVISIBLE);
                return true;
            }
        }
        return false;
    }

    public void loadCoinTokenArray(){
        //if (Setting.getInstance().mCoinTokenArray.size() > 0){
        //    return;
        //}

        Setting.getInstance().mCoinTokenArray.clear();


        new Thread(new Runnable() {
            @Override
            public void run() {
                mAccountManager.getSupportedCointokenArray(Setting.getInstance().mCoinTokenArray);

                for (int i=0; i<Setting.getInstance().mCoinTokenArray.size(); i++){
                    Log.i(TAG, Setting.getInstance().mCoinTokenArray.get(i).toString());

                    if (i==0){
                        mSelCoinTokenItem = Setting.getInstance().mCoinTokenArray.get(0);

                        if (mTextViewSelTokenItem!=null && mImageViewSelCoinLogo!=null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextViewSelTokenItem.setText(mSelCoinTokenItem.second);
                                    mImageViewSelCoinLogo.setImageResource(AccountUtil.getCoinLogo(mSelCoinTokenItem.first));
                                }
                            });
                        }

                    }
                }
            }
        }).start();
    }

    public void onRegisterButtonClick(View v)
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
            showAlert("Register failed!", "Invalid email account or password!");
            return;
        }

        if (mSelCoinTokenItem == null){
            showAlert("No token!", "Register abort.");
            return;
        }

        String email = mEdittextEmail.getText().toString();
        String pwd = mEdittextPwd.getText().toString();
        String phone = mEdittextPhone.getText().toString();

        //Setting.getInstance().mAccount = new SecuXUserAccount("maochuntest6@secuxtech.com", "0975123456", "12345678");
        final SecuXUserAccount account = new SecuXUserAccount(email, phone, pwd);

        CommonProgressDialog.showProgressDialog(getActivity(), "Register...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Pair<Integer, String> ret = mAccountManager.registerUserAccount(account, mSelCoinTokenItem.first, mSelCoinTokenItem.second);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CommonProgressDialog.dismiss();
                        if (ret.first == SecuXServerRequestHandler.SecuXRequestOK) {
                            account.mCoinAccountArr.clear();
                            login(account);
                        } else {
                            final String error = ret.second;
                            showAlert("Register failed!", "Error: " + error);
                        }
                    }
                });

            }
        }).start();
    }

    public void toggleCoinTokenListView(){

        hideKeyboard(this.mListViewCoinToken);

        int rowHeight = (int)SecuXUtility.convertDpToPixel(60, getActivity());
        int listHeight = 5 * rowHeight;
        if (Setting.getInstance().mCoinTokenArray.size() * rowHeight < listHeight)
            listHeight = Setting.getInstance().mCoinTokenArray.size() * rowHeight;

        ViewGroup.LayoutParams params = mListViewCoinToken.getLayoutParams();
        params.height = listHeight;

        /*
        ImageView imgview = getActivity().findViewById(R.id.imageView_cointokensel_downarrow);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(imgview, "rotation", 180f, 0f);
        rotate.setDuration(500);
        rotate.start();

         */

        ExpandCollapseAnimation animation = null;
        if (mShowCoinTokenSelList){
            Log.i(TAG, "close list");
            animation = new ExpandCollapseAnimation(mListViewCoinToken, 500, 1);
            mShowCoinTokenSelList = false;
        }else {
            Log.i(TAG, "show list");
            animation = new ExpandCollapseAnimation(mListViewCoinToken, 500, 0);
            mShowCoinTokenSelList = true;

        }
        mListViewCoinToken.startAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mShowCoinTokenSelList){
                    final SupportedCoinTokeListAdapter adapter = new SupportedCoinTokeListAdapter(getActivity());
                    mListViewCoinToken.setAdapter(adapter);
                }else{
                    mListViewCoinToken.setAdapter(null);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


    }

}
