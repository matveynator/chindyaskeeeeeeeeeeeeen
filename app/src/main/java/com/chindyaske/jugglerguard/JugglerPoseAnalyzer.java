package com.chindyaske.jugglerguard;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

final class JugglerPoseAnalyzer implements ImageAnalysis.Analyzer, AutoCloseable {
    interface Listener {
        void onRuleResult(WaistRuleResult waistRuleResult);

        void onAnalyzerError(String message);
    }

    private static final String MODEL_ASSET_PATH = "pose_landmarker_lite.task";
    private static final long MINIMUM_FRAME_INTERVAL_MS = 80L;

    private final CameraFrameBitmapConverter bitmapConverter = new CameraFrameBitmapConverter();
    private final WaistPositionRule waistPositionRule = new WaistPositionRule();
    private final PoseLandmarker poseLandmarker;
    private final Listener listener;

    private long lastAnalyzedFrameUptimeMs;
    private boolean closed;

    JugglerPoseAnalyzer(Context context, Listener listener) {
        this.listener = listener;

        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET_PATH)
                .build();
        PoseLandmarker.PoseLandmarkerOptions poseLandmarkerOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.55f)
                .setMinPosePresenceConfidence(0.55f)
                .setMinTrackingConfidence(0.55f)
                .setResultListener(this::handlePoseLandmarkerResult)
                .setErrorListener(this::handlePoseLandmarkerError)
                .build();

        poseLandmarker = PoseLandmarker.createFromOptions(context, poseLandmarkerOptions);
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (closed) {
            imageProxy.close();
            return;
        }

        long currentUptimeMs = SystemClock.uptimeMillis();
        if (currentUptimeMs - lastAnalyzedFrameUptimeMs < MINIMUM_FRAME_INTERVAL_MS) {
            imageProxy.close();
            return;
        }
        lastAnalyzedFrameUptimeMs = currentUptimeMs;

        try {
            Bitmap bitmap = bitmapConverter.copyRgbaFrameToBitmap(imageProxy);
            MPImage mediaPipeImage = new BitmapImageBuilder(bitmap).build();
            ImageProcessingOptions imageProcessingOptions = ImageProcessingOptions.builder()
                    .setRotationDegrees(imageProxy.getImageInfo().getRotationDegrees())
                    .build();
            poseLandmarker.detectAsync(mediaPipeImage, imageProcessingOptions, currentUptimeMs);
        } catch (RuntimeException runtimeException) {
            listener.onAnalyzerError(runtimeException.getMessage());
        } finally {
            imageProxy.close();
        }
    }

    @Override
    public void close() {
        closed = true;
        poseLandmarker.close();
    }

    private void handlePoseLandmarkerResult(PoseLandmarkerResult poseLandmarkerResult, MPImage ignoredMediaPipeImage) {
        if (closed) {
            return;
        }
        if (poseLandmarkerResult.landmarks().isEmpty()) {
            listener.onRuleResult(WaistRuleResult.waitingForPerson());
            return;
        }

        WaistRuleResult waistRuleResult = waistPositionRule.evaluate(poseLandmarkerResult.landmarks().get(0));
        listener.onRuleResult(waistRuleResult);
    }

    private void handlePoseLandmarkerError(RuntimeException runtimeException) {
        listener.onAnalyzerError(runtimeException.getMessage());
    }
}
