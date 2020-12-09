package com.example.hanming.utils;

import android.Manifest;
import android.app.Activity;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

/**
 * @author: xhSun
 * @time: 2020/10/28
 * @description:
 */
public class PermissionUtils {

    public static final int REQUEST_PERMISSION_CODE = 1;

    /**
     * 检查权限是否已申请成功
     */
    public static void checkPermission(Activity activity) {

        boolean isPermissionDone = SPUtils.getInstance(activity).getBoolean("permission");
        if (isPermissionDone) {
            return;
        }

        String[] permissionArray = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        requestPermission(activity, permissionArray);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Activity activity, String[] permissionArray) {
        if (permissionArray.length != 0) {
            ActivityCompat.requestPermissions(activity, permissionArray, REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * 弹出警告提示框
     */
    public static void showWaringDialog(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("警告！")
                .setMessage("请前往设置->应用->UtilsApplication->权限中打开相关权限，否则功能无法正常运行！")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 一般情况下如果用户不授权的话，功能是无法运行的，做退出处理
                    activity.finish();
                }).show();
    }
}
