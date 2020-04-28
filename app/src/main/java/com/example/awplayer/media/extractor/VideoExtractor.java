package com.example.awplayer.media.extractor;

import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.awplayer.media.IExtractor;

import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class VideoExtractor implements IExtractor {

    private MMExtractor mMediaExtractor;

    public VideoExtractor(String path) {
        mMediaExtractor = new MMExtractor(path);
    }

    @Override
    public MediaFormat getFormat() {
        return mMediaExtractor.getVideoFormat();
    }

    @Override
    public int readBuffer(ByteBuffer byteBuffer) {
        return mMediaExtractor.readBuffer(byteBuffer);
    }

    @Override
    public long getCurrentTimestamp() {
        return mMediaExtractor.getCurrentTimestamp();
    }

    @Override
    public long seek(long pos) {
        return mMediaExtractor.seek(pos);
    }

    @Override
    public void setStartPos(long pos) {
        mMediaExtractor.setStartPos(pos);
    }

    @Override
    public void stop() {
        mMediaExtractor.stop();
    }
}
