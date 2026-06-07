package com.chindyaske.jugglerguard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends ComponentActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 42;

    private PreviewView cameraPreviewView;
    private PoseOverlayView poseOverlayView;
    private TextView statusTextView;
    private ExecutorService cameraExecutorService;
    private WarningTonePlayer warningTonePlayer;
    private JugglerPoseAnalyzer jugglerPoseAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraExecutorService = Executors.newSingleThreadExecutor();
        warningTonePlayer = new WarningTonePlayer();

        buildScreen();

        if (cameraPermissionIsGranted()) {
            startFrontCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        if (jugglerPoseAnalyzer != null) {
            jugglerPoseAnalyzer.close();
        }
        warningTonePlayer.release();
        cameraExecutorService.shutdown();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CAMERA_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startFrontCamera();
            return;
        }
        showStatus(WaistRuleResult.error("Camera permission is required"));
    }

    private void buildScreen() {
        FrameLayout rootLayout = new FrameLayout(this);

        cameraPreviewView = new PreviewView(this);
        cameraPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        cameraPreviewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        rootLayout.addView(cameraPreviewView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        poseOverlayView = new PoseOverlayView(this);
        poseOverlayView.setMirrorHorizontally(true);
        rootLayout.addView(poseOverlayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        statusTextView = new TextView(this);
        statusTextView.setTextColor(Color.WHITE);
        statusTextView.setTextSize(20.0f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setBackgroundColor(Color.argb(150, 0, 0, 0));
        statusTextView.setPadding(24, 18, 24, 18);
        FrameLayout.LayoutParams statusLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        rootLayout.addView(statusTextView, statusLayoutParams);

        setContentView(rootLayout);
        showStatus(WaistRuleResult.waitingForPerson());
    }

    private boolean cameraPermissionIsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startFrontCamera() {
        try {
            jugglerPoseAnalyzer = new JugglerPoseAnalyzer(this, new JugglerPoseAnalyzer.Listener() {
                @Override
                public void onRuleResult(WaistRuleResult waistRuleResult) {
                    runOnUiThread(() -> handleWaistRuleResult(waistRuleResult));
                }

                @Override
                public void onAnalyzerError(String message) {
                    runOnUiThread(() -> showStatus(WaistRuleResult.error(message)));
                }
            });
        } catch (RuntimeException runtimeException) {
            showStatus(WaistRuleResult.error("Add pose_landmarker_lite.task to app/src/main/assets"));
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(
                () -> bindFrontCamera(cameraProviderFuture),
                ContextCompat.getMainExecutor(this)
        );
    }

    private void bindFrontCamera(ListenableFuture<ProcessCameraProvider> cameraProviderFuture) {
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();

            Preview cameraPreview = new Preview.Builder().build();
            cameraPreview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build();
            imageAnalysis.setAnalyzer(cameraExecutorService, jugglerPoseAnalyzer);

            cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    cameraPreview,
                    imageAnalysis
            );
        } catch (ExecutionException exception) {
            showStatus(WaistRuleResult.error(exception.getMessage()));
        } catch (InterruptedException exception) {
            showStatus(WaistRuleResult.error(exception.getMessage()));
            Thread.currentThread().interrupt();
        } catch (RuntimeException runtimeException) {
            showStatus(WaistRuleResult.error(runtimeException.getMessage()));
        }
    }

    private void handleWaistRuleResult(WaistRuleResult waistRuleResult) {
        poseOverlayView.setWaistRuleResult(waistRuleResult);
        showStatus(waistRuleResult);
        if (waistRuleResult.handsAboveWaist) {
            warningTonePlayer.playWarningIfReady();
        }
    }

    private void showStatus(WaistRuleResult waistRuleResult) {
        statusTextView.setText(waistRuleResult.message);
        int statusColor = waistRuleResult.handsAboveWaist ? Color.rgb(160, 20, 20) : Color.argb(150, 0, 0, 0);
        statusTextView.setBackgroundColor(statusColor);
    }
}
