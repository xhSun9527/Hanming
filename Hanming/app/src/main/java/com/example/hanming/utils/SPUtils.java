package com.example.hanming.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * @author: xhSun
 * @time: 2020/10/27
 * @description:
 */
public class SPUtils {
    private SharedPreferences sharedPreferences;
    private static final String SP_NAME = "xhsun";

    private static SPUtils mSPUtils = null;

    @SuppressLint("CommitPrefEdits")
    private SPUtils(Context context) {
        sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public static SPUtils getInstance(Context context) {
        if (mSPUtils == null) {
            synchronized (SPUtils.class) {
                if (mSPUtils == null) {
                    mSPUtils = new SPUtils(context);
                }
            }
        }
        return mSPUtils;
    }

    public void saveInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public void saveString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public void saveFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    public void saveBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public void saveLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public int getInt(String key) {
        try {
            checkKey(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sharedPreferences.getInt(key, 0);
    }

    public String getString(String key) {
        try {
            checkKey(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sharedPreferences.getString(key, null);
    }

    public boolean getBoolean(String key) {
        try {
            checkKey(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sharedPreferences.getBoolean(key, false);
    }

    public long getLong(String key) {
        try {
            checkKey(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sharedPreferences.getLong(key, 0);
    }

    private void checkKey(String key) throws Exception {
        if (TextUtils.isEmpty(key)) {
            throw new Exception("key is null");
        }
    }
}
