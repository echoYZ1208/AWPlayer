package com.example.awplayer.media.decode;

import android.content.SyncRequest;
import android.media.MediaFormat;

public interface IDecoder extends Runnable {
    /**
     * 暂停解码
     */
    void pause();

    /**
     * 继续解码
     */
    void goOn();

    /**
     * 跳转到指定位置
     * 并返回实际帧的时间
     *
     * @param pos: 毫秒
     * @return 实际时间戳，单位：毫秒
     */
    long seekTo(long pos);

    /**
     * 跳转到指定位置,并播放
     * 并返回实际帧的时间
     *
     * @param pos: 毫秒
     * @return 实际时间戳，单位：毫秒
     */
    long seekAndPlay(long pos);

    /**
     * 停止解码
     */
    void stop();

    /**
     * 是否正在解码
     */
    boolean isDecoding();

    /**
     * 是否正在快进
     */
    boolean isSeeking();

    /**
     * 是否停止解码
     */
    boolean isStop();

    /**
     * 设置尺寸监听器
     */
    void setSizeListener(IDecoderProgress l);

    /**
     * 设置状态监听器
     */
    void setStateListener(IDecoderStateListener l);

    /**
     * 获取视频宽
     */
    int getWidth();

    /**
     * 获取视频高
     */
    int getHeight();

    /**
     * 获取视频长度
     */
    long getDuration();

    /**
     * 当前帧时间，单位：ms
     */
    long getCurTimeStamp();

    /**
     * 获取视频旋转角度
     */
    int getRotationAngle();

    /**
     * 获取音视频对应的格式参数
     */
    MediaFormat getMediaFormat();

    /**
     * 获取音视频对应的媒体轨道
     */
    int getTrack();

    /**
     * 获取解码的文件路径
     */
    String getFilePath();

    /**
     * 无需音视频同步
     */
    IDecoder withoutSync();
}
