package com.example.awplayer.media.decode;

import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class Frame {
    public ByteBuffer buffer;

    public MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    void setBufferInfo(MediaCodec.BufferInfo info) {
        bufferInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags);
    }
}
