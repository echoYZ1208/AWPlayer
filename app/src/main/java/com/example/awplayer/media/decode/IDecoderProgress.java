package com.example.awplayer.media.decode;

public interface IDecoderProgress {
    /**
     * 视频宽高回调
     */
    void videoSizeChange(int width, int height, int rotationAngle);

    /**
     * 视频播放进度回调
     */
    void videoProgressChange(long pos);
}
