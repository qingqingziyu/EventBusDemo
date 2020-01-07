package com.cf.eventbusdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.cf.eventbus.EventBus;
import com.cf.eventbus.Subscribe;
import com.cf.eventbus.ThreadMode;

import static com.cf.eventbusdemo.TwoActivity.getCurrentMethodName;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();

    private TextView tv_one;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);

        tv_one = findViewById(R.id.tv_one);

        findViewById(R.id.btn_).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_go).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_:
                EventBus.getDefault().postSticky("小子，我是哥哥");
                break;
            case R.id.btn_2:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().postSticky("小子，我是哥哥");
                    }
                }).start();
                break;
            case R.id.btn_go:
                Intent intent = new Intent(this, TwoActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Subscribe
    public void setText(EventNotify str) {
        Log.e(TAG, "methodName: " + getCurrentMethodName() + " thread:" + Thread.currentThread().getId() + " msg:" + str);
        tv_one.setText("MainActivity:" + str);
    }


    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void setTextAsy(EventNotify str) {
        Log.e(TAG, "methodName: " + getCurrentMethodName() + " thread:" + Thread.currentThread().getId() + " msg:" + str);
        tv_one.setText("MainActivity:" + str);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void setTextInMain(EventNotify str) {
        Log.e(TAG, "methodName: " + getCurrentMethodName() + " thread:" + Thread.currentThread().getId() + " msg:" + str);
        tv_one.setText("MainActivity:" + str);
    }


    @Override
    protected void onDestroy() {
        EventBus.getDefault().unRegister(this);
        super.onDestroy();
    }
}
