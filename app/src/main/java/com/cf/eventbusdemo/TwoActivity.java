package com.cf.eventbusdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.cf.eventbus.EventBus;
import com.cf.eventbus.Subscribe;
import com.cf.eventbus.ThreadMode;

public class TwoActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();

    private TextView tv_two;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two);
        EventBus.getDefault().register(this);
        tv_two = findViewById(R.id.tv_two);
        findViewById(R.id.btn_).setOnClickListener(this);
        findViewById(R.id.btn_in).setOnClickListener(this);
        findViewById(R.id.btn_btn).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_:
                EventBus.getDefault().post(new EventNotify("哥哥，我才是你哥哥"));
                break;
            case R.id.btn_in:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().post(new EventNotify("哥哥，我才是你哥哥"));
                    }
                }).start();
                break;
            case R.id.btn_btn:
                finish();
                break;
        }
    }

    @Subscribe(sticky = true)
    public void setText(String str) {
        Log.e(TAG, "methodName: " + getCurrentMethodName() + " thread:" + Thread.currentThread().getId() + " msg:" + str);
        tv_two.setText("MainActivity:" + str);
    }


    @Subscribe(sticky = true,threadMode = ThreadMode.ASYNC)
    public void setTextASY(String async){
        Log.e(TAG, "methodName: " + getCurrentMethodName() + " thread:" + Thread.currentThread().getId() + " msg:" + async);
        tv_two.setText("MainActivity:" + async);
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN,priority = 1)
    public void setTextInMain(String str){
        Log.e(TAG, "methodName: " + getCurrentMethodName() + " thread:" + Thread.currentThread().getId() + " msg:" + str);
        tv_two.setText("MainActivity:" + str);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unRegister(this);
        super.onDestroy();
    }

    public static String getCurrentMethodName() {
        int level = 1;
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        String methodName = stacks[level].getMethodName();
        return methodName;
    }
}
