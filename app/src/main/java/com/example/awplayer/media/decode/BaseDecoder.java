package com.example.awplayer.media.decode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.awplayer.utils.ThreadUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
abstract class BaseDecoder implements IDecoder {

    private static final String TAG = "BaseDecoder";

    /**
     * 文件路径
     */
    private String mFilePath;

    //-------------线程相关------------------------
    /**
     * 解码器是否在运行
     */
    private boolean mIsRunning = true;

    /**
     * 线程等待锁
     */
    private final Object mLock = new Object();

    /**
     * 是否可以进入解码
     */
    private boolean mReadyForDecode;

    //---------------状态相关-----------------------
    /**
     * 音视频解码器
     */
    private MediaCodec mCodec;

    /**
     * 音视频数据读取器
     */
    private IExtractor mExtractor;

    /**
     * 解码输入缓存区
     */
    private List<ByteBuffer> mInputBuffers;

    /**
     * 解码输出缓存区
     */
    private List<ByteBuffer> mOutputBuffers;

    /**
     * 解码数据信息
     */
    private MediaCodec.BufferInfo mBufferInfo;

    private DecodeState mState = DecodeState.STOP;

    protected IDecoderStateListener mStateListener;

    /**
     * 流数据是否结束
     */
    private boolean mIsEOS;

    protected int mVideoWidth;

    protected int mVideoHeight;

    private long mDuration;

    private long mStartPos;

    private long mEndPos;

    /**
     * 开始解码时间，用于音视频同步
     */
    private long mStartTimeForSync = -1;

    // 是否需要音视频渲染同步
    private boolean mSyncRender = true;

    public BaseDecoder(String filePath) {
        mFilePath = filePath;
    }

    @Override
    public void run() {
        if (mState == DecodeState.STOP) {
            mState = DecodeState.START;
        }
        if (mStateListener != null) {
            mStateListener.onPrepare(this);
        }

        //【解码步骤：1. 初始化，并启动解码器】
        if (!init()) {
            return;
        }

        Log.i(TAG, "开始解码");
        try {
            while (mIsRunning) {
                // 判断是否需要阻塞
                if (mState != DecodeState.START && mState != DecodeState.DECODING && mState != DecodeState.SEEKING) {
                    Log.i(TAG, "进入等待：" + mState);

                    waitDecode();

                    // ---------【同步时间矫正】-------------
                    //恢复同步的起始时间，即去除等待流失的时间
                    mStartTimeForSync = System.currentTimeMillis() - getCurTimeStamp();
                }

                if (!mIsRunning || mState == DecodeState.STOP) {
                    mIsRunning = false;
                    break;
                }

                if (mStartTimeForSync == -1L) {
                    mStartTimeForSync = System.currentTimeMillis();
                }

                //如果数据没有解码完毕，将数据推入解码器解码
                if (!mIsEOS) {
                    //【解码步骤：2. 见数据压入解码器输入缓冲】
                    mIsEOS = pushBufferToDecoder();
                }

                //【解码步骤：3. 将解码好的数据从缓冲区拉取出来】
                int index = pullBufferFromDecoder();
                if (index >= 0) {
                    // ---------【音视频同步】-------------
                    if (mSyncRender && mState == DecodeState.DECODING) {
                        sleepRender();
                    }
                    //【解码步骤：4. 渲染】
                    if (mSyncRender) {// 如果只是用于编码合成新视频，无需渲染
                        render(mOutputBuffers.get(index), mBufferInfo);
                    }

                    //将解码数据传递出去
                    Frame frame = new Frame();
                    frame.buffer = mOutputBuffers.get(index);
                    frame.setBufferInfo(mBufferInfo);
                    if (mStateListener != null) {
                        mStateListener.onDecodeOneFrame(this, frame);
                    }

                    //【解码步骤：5. 释放输出缓冲】
                    mCodec.releaseOutputBuffer(index, true);

                    if (mState == DecodeState.START) {
                        mState = DecodeState.PAUSE;
                    }
                }
                //【解码步骤：6. 判断解码是否完成】
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.i(TAG, "解码结束");
                    mState = DecodeState.FINISH;
                    if (mStateListener != null) {
                        mStateListener.onFinish(this);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            doneDecode();
            release();
        }
    }

    private boolean init() {
        if (mFilePath.isEmpty() || !new File(mFilePath).exists()) {
            Log.w(TAG, "文件路径为空");
            if (mStateListener != null) {
                mStateListener.onError(this, "文件路径为空");
            }
            return false;
        }

        if (!check()) return false;

        //初始化数据提取器
        mExtractor = initExtractor(mFilePath);
        if (mExtractor == null || mExtractor.getFormat() == null) {
            Log.w(TAG, "无法解析文件");
            return false;
        }

        //初始化参数
        if (!initParams()) return false;

        //初始化渲染器
        if (!initRender()) return false;

        //初始化解码器
        if (!initCodec()) return false;
        return true;
    }

    private boolean initParams() {
        try {
            MediaFormat format = mExtractor.getFormat();
            mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000;
            if (mEndPos == 0L) mEndPos = mDuration;

            initSpecParams(mExtractor.getFormat());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initCodec() {
        try {
            String type = mExtractor.getFormat().getString(MediaFormat.KEY_MIME);
            if (type != null) {
                mCodec = MediaCodec.createDecoderByType(type);
            }
            if (!configCodec(mCodec, mExtractor.getFormat())) {
                waitDecode();
            }
            mCodec.start();

            mInputBuffers = Arrays.asList(mCodec.getInputBuffers());
            mOutputBuffers = Arrays.asList(mCodec.getOutputBuffers());
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    /**
     * 检查子类参数
     */
    abstract boolean check();

    /**
     * 初始化数据提取器
     */
    abstract IExtractor initExtractor(String path);

    /**
     * 初始化子类自己特有的参数
     */
    abstract void initSpecParams(MediaFormat format);

    /**
     * 配置解码器
     */
    abstract boolean configCodec(MediaCodec codec, MediaFormat format);

    /**
     * 初始化渲染器
     */
    abstract boolean initRender();

    /**
     * 渲染
     */
    abstract void render(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo);

    /**
     * 结束解码
     */
    abstract void doneDecode();
    /**
     * 解码线程进入等待
     */
    private void waitDecode() {
        try {
            if (mState == DecodeState.PAUSE) {
                if (mStateListener != null) {
                    mStateListener.onPause(this);
                }
            }
            synchronized(mLock) {
                mLock.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean pushBufferToDecoder() {
        int inputBufferIndex = mCodec.dequeueInputBuffer(1000);
        boolean isEndOfStream = false;

        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mInputBuffers.get(inputBufferIndex);
            int sampleSize = mExtractor.readBuffer(inputBuffer);

            if (sampleSize < 0) {
                //如果数据已经取完，压入数据结束标志：MediaCodec.BUFFER_FLAG_END_OF_STREAM
                mCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isEndOfStream = true;
            } else {
                mCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, mExtractor.getCurrentTimestamp(), 0);
            }
        }
        return isEndOfStream;
    }

    private int pullBufferFromDecoder() {
        // 查询是否有解码完成的数据，index >=0 时，表示数据有效，并且index为缓冲区索引
        int index = mCodec.dequeueOutputBuffer(mBufferInfo, 1000);
        switch (index) {
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                break;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                mOutputBuffers = Arrays.asList(mCodec.getOutputBuffers());
                break;
        }
        return index;
    }

    private void sleepRender() {
        long passTime = System.currentTimeMillis() - mStartTimeForSync;
        long curTime = getCurTimeStamp();
        if (curTime > passTime) {
            ThreadUtil.sleep(curTime - passTime);
        }
    }

    private void release() {
        try {
            Log.i(TAG, "解码停止，释放解码器");
            mState = DecodeState.STOP;
            mIsEOS = false;
            mExtractor.stop();
            mCodec.stop();
            mCodec.release();
            if (mStateListener != null) {
                mStateListener.onDestroy(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void goOn() {

    }

    @Override
    public long seekTo(long pos) {
        return 0;
    }

    @Override
    public long seekAndPlay(long pos) {
        return 0;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isDecoding() {
        return false;
    }

    @Override
    public boolean isSeeking() {
        return false;
    }

    @Override
    public boolean isStop() {
        return false;
    }

    @Override
    public void setSizeListener(IDecoderProgress l) {

    }

    @Override
    public void setStateListener(IDecoderStateListener l) {

    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public long getCurTimeStamp() {
        return mBufferInfo.presentationTimeUs / 1000;
    }

    @Override
    public int getRotationAngle() {
        return 0;
    }

    @Override
    public MediaFormat getMediaFormat() {
        return null;
    }

    @Override
    public int getTrack() {
        return 0;
    }

    @Override
    public String getFilePath() {
        return null;
    }

    @Override
    public IDecoder withoutSync() {
        return null;
    }
}
