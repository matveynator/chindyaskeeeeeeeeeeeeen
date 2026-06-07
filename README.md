# Juggler Guard

Android app for a juggling drill: the front camera watches one person and plays a short warning tone when either wrist goes above the estimated belt line.

## How it works

The app uses CameraX for the front camera and MediaPipe Pose Landmarker for on-device body landmarks. The rule is deliberately simple:

1. Detect shoulders, hips, and wrists.
2. Estimate the belt line between the shoulder line and hip line.
3. Play a warning tone when a wrist is visibly above that line.

The screen also draws the belt line and wrist markers. Green means the hands are below the belt line. Red means the signal condition is active.

## Build

Open this folder in Android Studio and run the `app` configuration on a physical Android device with a front camera.

The MediaPipe model is already included here:

`app/src/main/assets/pose_landmarker_lite.task`

If you ever need to replace it, download the Lite task model from:

`https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task`

## Notes

The signal is rate-limited so it does not become a continuous tone. The default threshold lives in `WaistPositionRule.java`:

`HAND_MARGIN_ABOVE_WAIST`
