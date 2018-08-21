package com.github.guilhermesgb.steward.utils;

import android.os.Handler;
import android.os.Looper;

public class RunnableUtils {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DelayCallback {

        void run();

    }

    public static void runOnUiThreadAfterDelay(int milliseconds, final DelayCallback callback) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.run();
            }
        }, milliseconds);
    }

}
