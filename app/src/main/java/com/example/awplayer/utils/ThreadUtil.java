package com.example.awplayer.utils;

public class ThreadUtil {

    public static void sleep(long pos) {
        try {
            Thread.sleep(pos);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
