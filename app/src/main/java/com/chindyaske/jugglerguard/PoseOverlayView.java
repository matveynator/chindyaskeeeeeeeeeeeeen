package com.chindyaske.jugglerguard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

final class PoseOverlayView extends View {
    private final Paint beltLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wristPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private WaistRuleResult waistRuleResult = WaistRuleResult.waitingForPerson();
    private boolean mirrorHorizontally;

    PoseOverlayView(Context context) {
        super(context);
        configurePaint();
    }

    PoseOverlayView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        configurePaint();
    }

    void setMirrorHorizontally(boolean mirrorHorizontally) {
        this.mirrorHorizontally = mirrorHorizontally;
        invalidate();
    }

    void setWaistRuleResult(WaistRuleResult waistRuleResult) {
        this.waistRuleResult = waistRuleResult;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!waistRuleResult.personDetected || waistRuleResult.waistLineY < 0.0f) {
            return;
        }

        int signalColor = waistRuleResult.handsAboveWaist ? Color.rgb(255, 59, 48) : Color.rgb(52, 199, 89);
        beltLinePaint.setColor(signalColor);
        wristPaint.setColor(signalColor);

        float lineY = waistRuleResult.waistLineY * getHeight();
        canvas.drawRect(0.0f, lineY - 5.0f, getWidth(), lineY + 5.0f, backgroundPaint);
        canvas.drawLine(0.0f, lineY, getWidth(), lineY, beltLinePaint);

        drawWrist(canvas, waistRuleResult.leftWrist);
        drawWrist(canvas, waistRuleResult.rightWrist);
    }

    private void configurePaint() {
        beltLinePaint.setStrokeWidth(8.0f);
        beltLinePaint.setStyle(Paint.Style.STROKE);

        wristPaint.setStyle(Paint.Style.FILL);

        backgroundPaint.setColor(Color.argb(110, 0, 0, 0));
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    private void drawWrist(Canvas canvas, PointF normalizedWristPoint) {
        if (normalizedWristPoint == null) {
            return;
        }
        float normalizedX = mirrorHorizontally ? 1.0f - normalizedWristPoint.x : normalizedWristPoint.x;
        canvas.drawCircle(normalizedX * getWidth(), normalizedWristPoint.y * getHeight(), 16.0f, wristPaint);
    }
}
