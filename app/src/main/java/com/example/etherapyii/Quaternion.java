package com.example.etherapyii;

public class Quaternion {
    private float w, x, y, z;

    public Quaternion(float w, float x, float y, float z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float w(){
        return w;
    }
    public float x(){
        return x;
    }
    public float y(){
        return y;
    }

    public  float z(){
        return z;
    }

    public String toString(){
        return "Q0: "+ w+" Q1: "+x+" Q2: "+y+" Q3: "+z;
    }



}
