package com.sdcard.reader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int PERMISSIONS_REQ_CODE = 100;

    private static final String[] APP_NEED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private CheckPermissionCallback callback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.wtf(TAG, "Build.VERSION.SDK_INT= " + Build.VERSION.SDK_INT);

        callback = new PermissionsCallback();

        findViewById(R.id.btn_write_sd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkPermissions(callback);
            }
        });
    }

    //
    private void checkPermissions(CheckPermissionCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!Environment.isExternalStorageLegacy()) {
                // Android Q 沙盒存储模式

                // 1. 只能写到外存/Android/data/包名 下面的目录
                Toast.makeText(this, "Android Q - Scoped Storage Mode", Toast.LENGTH_SHORT).show();


                try {
                    copyFile(considerFileLocation());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 2. MediaStore 写在Download目录下

                return;
            }
        }

        // Q兼容模式 与 6.0以上动态权限一致
        Toast.makeText(this, "External Storage Legacy Mode", Toast.LENGTH_SHORT).show();

        boolean isExistNoGrantedPermission = false;
        for (String permission : APP_NEED_PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                isExistNoGrantedPermission = true;
                break;
            }
        }

        if (isExistNoGrantedPermission) {
            callback.onPermissionNotGranted();

        } else {
            callback.onPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQ_CODE:

                boolean isAllPermissionGranted = true;

                int pos = 0;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        isAllPermissionGranted = false;

                        boolean showHint = shouldShowRequestPermissionRationale(permissions[pos]);
                        if (showHint) {
                            Toast.makeText(this, "提示用户为什么一定要使用该权限", Toast.LENGTH_SHORT).show();
                            callback.onPermissionNotGranted();
                        }

                        break;
                    }
                    pos++;
                }

                if (isAllPermissionGranted)
                    callback.onPermissionGranted();

                break;

        }

    }

    private String considerFileLocation() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean externalStorageLegacy = Environment.isExternalStorageLegacy();
            if (!externalStorageLegacy) {
                // Android Q 非兼容模式需要特殊处理
                File targetFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "data_resource.txt");
                return targetFile.getAbsolutePath();
            }
        }

        // Android M -> P 及 Android Q 兼容模式 均使用原有动态权限进行处理 直接用File操作
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/demo");
        if (!dir.exists()) {
            if (!dir.mkdirs()) throw new IllegalAccessException("create target folder failed");
        }

        File targetFile = new File(dir, "data_resource.txt");
        return targetFile.getAbsolutePath();

    }

    private void copyFile(String targetPath) throws Exception {
        InputStream inputStream = getResources().openRawResource(R.raw.data_resource);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        bufferedReader.close();
        inputStream.close();

        FileOutputStream fos = new FileOutputStream(targetPath, false);
        fos.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        fos.flush();
        fos.close();
    }


    interface CheckPermissionCallback {

        void onPermissionGranted();

        void onPermissionNotGranted();
    }

    private class PermissionsCallback implements CheckPermissionCallback {
        @Override
        public void onPermissionGranted() {

            try {
                copyFile(considerFileLocation());
                Toast.makeText(MainActivity.this, "Copy Successful", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPermissionNotGranted() {
            requestPermissions(APP_NEED_PERMISSIONS, PERMISSIONS_REQ_CODE);
        }
    }
}
