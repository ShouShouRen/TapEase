package com.example.appbarpoc.ui.relaxation;

import android.os.Looper;
import android.os.Handler;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RelaxationViewModel extends ViewModel {

    private final MutableLiveData<String> breathingPhase = new MutableLiveData<>();
    private final MutableLiveData<Integer> timeRemaining = new MutableLiveData<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private int cycleCount = 0;

    public LiveData<String> getBreathingPhase() {
        return breathingPhase;
    }

    public LiveData<Integer> getTimeRemaining() {
        return timeRemaining;
    }

    public void startBreathing() {
        if (isRunning) return;

        isRunning = true;
        cycleCount = 0;
        runBreathingCycle();
    }

    private void runBreathingCycle() {
        if (cycleCount >= 3) {
            breathingPhase.postValue("結束");
            isRunning = false;
            return;
        }

        // 吸氣
        startPhase("吸氣", 4, () -> {
            // 閉氣
            startPhase("閉氣", 7, () -> {
                // 吐氣
                startPhase("吐氣", 8, () -> {
                    cycleCount++;
                    runBreathingCycle(); // 下一循環
                });
            });
        });
    }

    private void startPhase(String phase, int seconds, Runnable nextPhase) {
        breathingPhase.postValue(phase);
        countdown(seconds, nextPhase);
    }

    private void countdown(int seconds, Runnable onFinish) {
        final int[] time = {seconds};
        timeRemaining.postValue(time[0]);

        Runnable phaseRunnable = new Runnable() {
            @Override
            public void run() {
                time[0]--;
                if (time[0] > 0) {
                    timeRemaining.postValue(time[0]);
                    handler.postDelayed(this, 1000);
                } else {
                    timeRemaining.postValue(0);
                    onFinish.run();
                }
            }
        };
        handler.postDelayed(phaseRunnable, 1000);
    }

    public void reset() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        breathingPhase.postValue("");
        timeRemaining.postValue(0);
    }
}
