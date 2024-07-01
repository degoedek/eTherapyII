package com.example.etherapyii;

public class Quaternion {
    private float w, x, y, z;

    public Quaternion(float w, float x, float y, float z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float w() {return w;}
    public float x() {return x;}
    public float y() {return y;}

    public  float z() {return z;}

    public String toString(){
        return "Q0: "+ String.format("%.3f", w) +" Q1: "+ String.format("%.3f", x) +" Q2: "+ String.format("%.3f", y) +" Q3: " + String.format("%.3f", z);
    }

    public float norm() {
        return (float) Math.sqrt(w * w + x * x + y * y + z * z);
    }

    public Quaternion conjugate() {
        return new Quaternion(w, -x, -y, -z);
    }

    public Quaternion multiply(Quaternion q) {
        return new Quaternion(
                w * q.w - x * q.x - y * q.y - z * q.z,
                w * q.x + x * q.w + y * q.z - z * q.y,
                w * q.y - x * q.z + y * q.w + z * q.x,
                w * q.z + x * q.y - y * q.x + z * q.w
        );
    }

    public float angle() {
        // Ensure the quaternion is normalized
        float norm = norm();
        float normalizedW = w / norm;
        return (float) (2 * Math.acos(normalizedW) * 180 / Math.PI);
    }

    public double[] getXAxis() {
        return new double[]{
                1 - 2 * (y * y + z * z),
                2 * (x * y - w * z),
                2 * (x * z + w * y)
        };
    }

    // Method to extract the z-axis vector from a quaternion
    public double[] getZAxis() {
        return new double[]{
                2 * (x * z + w * y),
                2 * (y * z - w * x),
                1 - 2 * (x * x + y * y)
        };
    }

    public static double angleBetween(Quaternion q1, Quaternion q2) {
        double dot = q1.dot(q2);
        double magnitudeProduct = q1.magnitude() * q2.magnitude();
        return Math.acos(dot / magnitudeProduct) * 2.0; // angle in radians
    }

    // Calculate the magnitude of the quaternion
    public double magnitude() {
        return Math.sqrt(w * w + x * x + y * y + z * z);
    }

    // Calculate the dot product between two quaternions
    public double dot(Quaternion q) {
        return w * q.w + x * q.x + y * q.y + z * q.z;
    }

    Quaternion rotateY(float angleRad) {
        float sinHalfAngle = (float)Math.sin(angleRad / 2);
        float cosHalfAngle = (float)Math.cos(angleRad / 2);

        float newX = y * sinHalfAngle + w * cosHalfAngle + x * cosHalfAngle;
        float newY = -x * sinHalfAngle + w * cosHalfAngle + y * cosHalfAngle;
        float newZ = z * cosHalfAngle + w * sinHalfAngle;
        float newW = -z * sinHalfAngle + w * cosHalfAngle;

        return new Quaternion(newX, newY, newZ, newW);
    }

    // Method to rotate the quaternion by a given angle around the Z-axis
    Quaternion rotateZ(float angleRad) {
        float sinHalfAngle =(float) Math.sin(angleRad / 2);
        float cosHalfAngle = (float)Math.cos(angleRad / 2);

        float newX = x * cosHalfAngle + y * sinHalfAngle;
        float newY = -x * sinHalfAngle + y * cosHalfAngle;
        float newZ = z * cosHalfAngle + w * sinHalfAngle;
        float newW = -z * sinHalfAngle + w * cosHalfAngle;

        return new Quaternion(newX, newY, newZ, newW);
    }


}
