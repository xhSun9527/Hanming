package com.example.hanming.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hanming.R;
import com.example.hanming.utils.ImageDataUtils;
import com.example.hanming.utils.ImagePHash;
import com.example.hanming.utils.MIUtils;
import com.example.hanming.utils.PermissionUtils;
import com.example.hanming.utils.SPUtils;

import java.util.Map;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private TextView tvResult;
    private LinearLayout llContainer;
    private ImageView img1, img2;
    private static final int IMAGE_NUMBER_1 = 1;
    private static final int IMAGE_NUMBER_2 = 2;
    private int currentImage;
    private String imgPath1, imgPath2;

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Map<String, Object> resultMap = (Map<String, Object>) msg.obj;
                int diff = (int) resultMap.get("diff");
                float result = (float) resultMap.get("result");
                long costTime = (long) resultMap.get("time");
                tvResult.setVisibility(View.VISIBLE);
                tvResult.setText("不同像素点个数：" + diff + "\n相似度：" + result + "\n总耗时：" + costTime + "ms");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtils.checkPermission(this);
    }

    void initView() {
        Button btnChoose1 = findViewById(R.id.btnChooseImg1);
        Button btnChoose2 = findViewById(R.id.btnChooseImg2);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnClear = findViewById(R.id.btnClear);

        tvResult = findViewById(R.id.tvResult);
        llContainer = findViewById(R.id.imgContainer);
        img1 = findViewById(R.id.img1);
        img2 = findViewById(R.id.img2);

        btnChoose1.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnChoose2.setOnClickListener(this);
        btnClear.setOnClickListener(this);
    }

    @Override
    void initData() {

    }

    @Override
    int addContentLayout() {
        return R.layout.activity_main;
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnChooseImg1:
                imgPath1 = null;
                currentImage = IMAGE_NUMBER_1;
                ImageDataUtils.openAlbum(this);
                break;
            case R.id.btnChooseImg2:
                imgPath2 = null;
                currentImage = IMAGE_NUMBER_2;
                ImageDataUtils.openAlbum(this);
                break;
            case R.id.btnStart:
                if (TextUtils.isEmpty(imgPath1) || TextUtils.isEmpty(imgPath2)) {
                    Toast.makeText(this, "尚未选择对比图", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    ImagePHash pHash = new ImagePHash();
                    Map<String, Object> resultMap = pHash.startCompare(imgPath1, imgPath2);

                    Message message = new Message();
                    message.what = 1;
                    message.obj = resultMap;
                    handler.sendMessage(message);
                }).start();

                break;
            case R.id.btnClear:
                clearData();
                break;
        }
    }


    private void clearData() {

        if (TextUtils.isEmpty(imgPath1) || TextUtils.isEmpty(imgPath2)){
            Toast.makeText(this, "无需清理数据", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap leftBitmap = ((BitmapDrawable) img1.getDrawable()).getBitmap();
        Bitmap rightBitmap = ((BitmapDrawable) img2.getDrawable()).getBitmap();

        img1.setImageBitmap(null);
        if (leftBitmap != null && !leftBitmap.isRecycled()) {
            leftBitmap.recycle();
        }

        img2.setImageBitmap(null);
        if (rightBitmap != null && !rightBitmap.isRecycled()) {
            rightBitmap.recycle();
        }

        imgPath1 = null;
        imgPath2 = null;
        llContainer.setVisibility(View.GONE);
        tvResult.setVisibility(View.GONE);
    }

    /**
     * 展示图片
     */
    private void displayImage(String imagePath, ImageView imageView) {
        if (imagePath != null) {
            Bitmap bitImage = BitmapFactory.decodeFile(imagePath);//格式化图片

            imageView.setImageBitmap(bitImage);//为imageView设置图片
            llContainer.setVisibility(View.VISIBLE);

        } else {
            Toast.makeText(MainActivity.this, "获取图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 权限申请结果反馈
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtils.REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限申请成功", Toast.LENGTH_SHORT).show();
                SPUtils.getInstance(this).saveBoolean("permission", true);
            } else {
                PermissionUtils.showWaringDialog(this);
            }
        }
    }

    /**
     * 图片选取结果反馈
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (requestCode == ImageDataUtils.CHOOSE_PHOTO) {

            switch (currentImage) {
                case IMAGE_NUMBER_1:
                    imgPath1 = ImageDataUtils.readImage(this, data);
                    displayImage(imgPath1, img1);
                    break;
                case IMAGE_NUMBER_2:
                    imgPath2 = ImageDataUtils.readImage(this, data);
                    displayImage(imgPath2, img2);
                    break;
            }
        }
    }

    //双击退出程序
    private static final int TIME_EXIT = 2000;
    private long mBackPressed;

    @Override
    public void onBackPressed() {
        if (mBackPressed + TIME_EXIT > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "再点击一次返回退出程序", Toast.LENGTH_SHORT).show();
            mBackPressed = System.currentTimeMillis();
        }
    }

}