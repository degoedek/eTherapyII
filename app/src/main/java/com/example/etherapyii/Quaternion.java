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



}
