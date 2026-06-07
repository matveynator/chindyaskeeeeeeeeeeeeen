package com.chindyaske.jugglerguard;

import android.graphics.PointF;

final class WaistRuleResult {
    final boolean personDetected;
    final boolean handsAboveWaist;
    final float waistLineY;
    final PointF leftWrist;
    final PointF rightWrist;
    final String message;

    WaistRuleResult(
            boolean personDetected,
            boolean handsAboveWaist,
            float waistLineY,
            PointF leftWrist,
            PointF rightWrist,
            String message
    ) {
        this.personDetected = personDetected;
        this.handsAboveWaist = handsAboveWaist;
        this.waistLineY = waistLineY;
        this.leftWrist = leftWrist;
        this.rightWrist = rightWrist;
        this.message = message;
    }

    static WaistRuleResult waitingForPerson() {
        return new WaistRuleResult(false, false, -1.0f, null, null, "Stand in front of the camera");
    }

    static WaistRuleResult error(String message) {
        return new WaistRuleResult(false, false, -1.0f, null, null, message);
    }
}
