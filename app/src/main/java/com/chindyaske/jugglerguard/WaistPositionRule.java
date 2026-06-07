package com.chindyaske.jugglerguard;

import android.graphics.PointF;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;
import java.util.Optional;

final class WaistPositionRule {
    private static final int LEFT_SHOULDER = 11;
    private static final int RIGHT_SHOULDER = 12;
    private static final int LEFT_WRIST = 15;
    private static final int RIGHT_WRIST = 16;
    private static final int LEFT_HIP = 23;
    private static final int RIGHT_HIP = 24;

    private static final float MINIMUM_LANDMARK_VISIBILITY = 0.45f;
    private static final float WAIST_POSITION_FROM_SHOULDERS_TO_HIPS = 0.62f;
    private static final float HAND_MARGIN_ABOVE_WAIST = 0.03f;

    WaistRuleResult evaluate(List<NormalizedLandmark> poseLandmarks) {
        if (poseLandmarks.size() <= RIGHT_HIP) {
            return WaistRuleResult.waitingForPerson();
        }

        NormalizedLandmark leftShoulder = poseLandmarks.get(LEFT_SHOULDER);
        NormalizedLandmark rightShoulder = poseLandmarks.get(RIGHT_SHOULDER);
        NormalizedLandmark leftHip = poseLandmarks.get(LEFT_HIP);
        NormalizedLandmark rightHip = poseLandmarks.get(RIGHT_HIP);
        NormalizedLandmark leftWrist = poseLandmarks.get(LEFT_WRIST);
        NormalizedLandmark rightWrist = poseLandmarks.get(RIGHT_WRIST);

        if (!requiredLandmarksAreVisible(leftShoulder, rightShoulder, leftHip, rightHip, leftWrist, rightWrist)) {
            return WaistRuleResult.waitingForPerson();
        }

        float shoulderLineY = average(leftShoulder.y(), rightShoulder.y());
        float hipLineY = average(leftHip.y(), rightHip.y());
        float waistLineY = shoulderLineY + ((hipLineY - shoulderLineY) * WAIST_POSITION_FROM_SHOULDERS_TO_HIPS);

        boolean leftHandAboveWaist = leftWrist.y() < waistLineY - HAND_MARGIN_ABOVE_WAIST;
        boolean rightHandAboveWaist = rightWrist.y() < waistLineY - HAND_MARGIN_ABOVE_WAIST;
        boolean handsAboveWaist = leftHandAboveWaist || rightHandAboveWaist;
        String message = handsAboveWaist ? "Hands are above the belt" : "Hands are below the belt";

        return new WaistRuleResult(
                true,
                handsAboveWaist,
                waistLineY,
                new PointF(leftWrist.x(), leftWrist.y()),
                new PointF(rightWrist.x(), rightWrist.y()),
                message
        );
    }

    private static boolean requiredLandmarksAreVisible(NormalizedLandmark... landmarks) {
        for (NormalizedLandmark landmark : landmarks) {
            if (!landmarkIsVisible(landmark)) {
                return false;
            }
        }
        return true;
    }

    private static boolean landmarkIsVisible(NormalizedLandmark landmark) {
        Optional<Float> visibility = landmark.visibility();
        return !visibility.isPresent() || visibility.get() >= MINIMUM_LANDMARK_VISIBILITY;
    }

    private static float average(float firstNumber, float secondNumber) {
        return (firstNumber + secondNumber) / 2.0f;
    }
}
