package com.example.awplayer.media.decode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

import com.example.awplayer.media.BaseDecoder;
import com.example.awplayer.media.IExtractor;
import com.example.awplayer.media.extractor.VideoExtractor;

import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class VideoDecoder extends BaseDecoder {

    private static final String TAG = "VideoDecoder";

    private SurfaceView mSurfaceView;

    private Surface mSurface;

    public VideoDecoder(String filePath, SurfaceView surfaceView, Surface surface) {
        super(filePath);
        mSurfaceView = surfaceView;
        mSurface = surface;
    }

    @Override
    protected boolean check() {
        if (mSurfaceView == null && mSurface == null) {
            Log.w(TAG, "SurfaceView和Surface都为空，至少需要一个不为空");
            if (mStateListener != null) {
                mStateListener.onError(this, "显示器为空");
            }
            return false;
        }
        return true;
    }

    @Override
    protected IExtractor initExtractor(String path) {
        return new VideoExtractor(path);
    }

    @Override
    protected void initSpecParams(MediaFormat format) {

    }

    @Override
    protected boolean configCodec(final MediaCodec codec, final MediaFormat format) {
        if (mSurface != null) {
            codec.configure(format, mSurface , null, 0);
            notifyDecode();
        } else if (mSurfaceView.getHolder().getSurface() != null) {
            mSurface = mSurfaceView.getHolder().getSurface();
            configCodec(codec, format);
        } else {
            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback2() {
                @Override
                public void surfaceRedrawNeeded(SurfaceHolder holder) {

                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mSurface = holder.getSurface();
                    configCodec(codec, format);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {

                }
            });

            return false;
        }
        return true;
    }

    @Override
    protected boolean initRender() {
        return true;
    }

    @Override
    protected void render(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {

    }

    @Override
    protected void doneDecode() {

    }
}
