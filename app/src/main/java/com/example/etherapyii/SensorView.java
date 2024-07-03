package com.example.etherapyii;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.View;

public class SensorView extends View {
    private Paint paint;
    private PointF point;

    public SensorView(Context context) {
        super(context);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle((Paint.Style.FILL));
    }

    public void updateOrientation(Quaternion quaternion) {
        point = QuaternionTo2D.quaternionTo2DPoint(quaternion);
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (point != null) {
            canvas.drawCircle(getWidth() / 2 + point.x, getHeight() / 2 + point.y, 10, paint);
        }
    }
}
