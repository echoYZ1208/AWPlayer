package com.example.awplayer.media;

public interface IDecoderStateListener {

    void onPrepare(BaseDecoder decoder);

    void onReady(BaseDecoder decoder);

    void onRunning(BaseDecoder decoder);

    void onPause(BaseDecoder decoder);

    void onDecodeOneFrame(BaseDecoder decoder, Frame frame);

    void onFinish(BaseDecoder decoder);

    void onDestroy(BaseDecoder decoder);

    void onError(BaseDecoder decoder, String msg);
}
