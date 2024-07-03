package com.example.etherapyii;

import android.graphics.PointF;
import android.util.Log;

public class QuaternionTo2D {
    public static PointF quaternionTo2DPoint(Quaternion q) {
        float angleX = (float) Math.atan2(2.0f * (q.w() * q.x() + q.y() * q.z()), 1.0f - 2.0f * (q.x() * q.x() + q.y() * q.y()));
        float angleY = (float) Math.asin(2.0f * (q.w() * q.y() - q.z() * q.x()));

        // Mapping angles to screen coordinates
        float x = (float) Math.sin(angleX);
        float y = (float) Math.sin(angleY);

        Log.i("QuaternionTo2D", "Quaternion: " + q + " -> PointF: x=" + x + ", y=" + y);
        return new PointF(x, y);
    }
}
