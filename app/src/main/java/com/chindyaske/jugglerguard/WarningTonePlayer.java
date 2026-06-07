package com.chindyaske.jugglerguard;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;

final class WarningTonePlayer {
    private static final int TONE_DURATION_MS = 120;
    private static final long TONE_COOLDOWN_MS = 650L;

    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private long lastToneUptimeMs;

    void playWarningIfReady() {
        long currentUptimeMs = SystemClock.uptimeMillis();
        if (currentUptimeMs - lastToneUptimeMs < TONE_COOLDOWN_MS) {
            return;
        }
        lastToneUptimeMs = currentUptimeMs;
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, TONE_DURATION_MS);
    }

    void release() {
        toneGenerator.release();
    }
}
