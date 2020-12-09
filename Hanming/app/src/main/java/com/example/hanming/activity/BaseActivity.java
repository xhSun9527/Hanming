package com.example.hanming.activity;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

public abstract class BaseActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(addContentLayout());
        initView();
        initData();
    }

    abstract void initView();

    abstract void initData();

    abstract int addContentLayout();
}
