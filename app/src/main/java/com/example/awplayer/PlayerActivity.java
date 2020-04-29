package com.example.awplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;

import com.example.awplayer.media.decode.AudioDecoder;
import com.example.awplayer.media.decode.VideoDecoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PlayerActivity extends AppCompatActivity {

    ///sdcard/trailer.mp4
    private final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/trailer.mp4";

    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;

    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        initPlayer();

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }
    }

    private void initPlayer() {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        mVideoDecoder = new VideoDecoder(filePath, mSurfaceView, null);
        threadPool.execute(mVideoDecoder);

        mAudioDecoder = new AudioDecoder(filePath);
        threadPool.execute(mAudioDecoder);

        mVideoDecoder.goOn();
        mAudioDecoder.goOn();
    }

    public void clickRepack(View view) {
        repack();
    }

    private void repack() {
//        val repack = MP4Repack(path)
//        repack.start()
    }

    @Override
    protected void onDestroy() {
        mVideoDecoder.stop();
        mAudioDecoder.stop();
        super.onDestroy();
    }
}
