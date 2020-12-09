package com.example.hanming.activity;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hanming.utils.ImagePHash;
import com.example.hanming.utils.PermissionUtils;
import com.example.hanming.R;
import com.example.hanming.utils.SPUtils;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvResult;
    private LinearLayout llContainer;
    private ImageView img1, img2;
    private static final int CHOOSE_PHOTO = 101;
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
                tvResult.setText("不同像素点个数：" + diff + "\n相似度：" + result + "\n总耗时：" + costTime+"ms");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionUtils.checkPermission(this);
        initView();
    }

    private void initView() {
        Button btnChoose1 = findViewById(R.id.btnChooseImg1);
        Button btnChoose2 = findViewById(R.id.btnChooseImg2);
        Button btnStart = findViewById(R.id.btnStart);
        tvResult = findViewById(R.id.tvResult);
        llContainer = findViewById(R.id.imgContainer);
        img1 = findViewById(R.id.img1);
        img2 = findViewById(R.id.img2);

        btnChoose1.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnChoose2.setOnClickListener(this);
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnChooseImg1:
                imgPath1 = null;
                currentImage = IMAGE_NUMBER_1;
                openAlbum();
                break;
            case R.id.btnChooseImg2:
                imgPath2 = null;
                currentImage = IMAGE_NUMBER_2;
                openAlbum();
                break;
            case R.id.btnStart:

                new Thread(() -> {
                    ImagePHash pHash = new ImagePHash();
                    Map<String, Object> resultMap = pHash.startCompare(imgPath1, imgPath2);

                    Message message = new Message();
                    message.what = 1;
                    message.obj = resultMap;
                    handler.sendMessage(message);
                }).start();

                break;
        }
    }

    /**
     * 打开相册，选取图片
     */
    public void openAlbum() {
        //通过intent打开相册，使用startactivityForResult方法启动actvity，会返回到onActivityResult方法，所以我们还得复写onActivityResult方法
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    /**
     * 读取图片路径
     *
     * @param data
     */
    private void readImage(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;

                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android,providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }

        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        }

        switch (currentImage) {
            case IMAGE_NUMBER_1:
                imgPath1 = imagePath;
                displayImage(imgPath1, img1);
                break;
            case IMAGE_NUMBER_2:
                imgPath2 = imagePath;
                displayImage(imgPath2, img2);
                break;
        }

    }

    //获得图片路径
    public String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null); //内容提供器
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)); //获取路径
            }
        }
        cursor.close();
        return path;
    }

    private void displayImage(String imagePath, ImageView imageView) {
        if (imagePath != null) {
            Bitmap bitImage = BitmapFactory.decodeFile(imagePath);//格式化图片

            imageView.setImageBitmap(bitImage);//为imageView设置图片
            llContainer.setVisibility(View.VISIBLE);

        } else {
            Toast.makeText(MainActivity.this, "获取图片失败", Toast.LENGTH_SHORT).show();
        }
    }

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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_PHOTO) {
            readImage(data);
        }
    }

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