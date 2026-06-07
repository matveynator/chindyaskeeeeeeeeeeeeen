package com.chindyaske.jugglerguard;

import android.graphics.Bitmap;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

final class CameraFrameBitmapConverter {
    Bitmap copyRgbaFrameToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] imagePlanes = imageProxy.getPlanes();
        if (imagePlanes.length != 1) {
            throw new IllegalArgumentException("Camera analysis must use RGBA_8888 output");
        }

        ByteBuffer rgbaBuffer = imagePlanes[0].getBuffer();
        rgbaBuffer.rewind();

        Bitmap bitmap = Bitmap.createBitmap(
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(rgbaBuffer);
        return bitmap;
    }
}
