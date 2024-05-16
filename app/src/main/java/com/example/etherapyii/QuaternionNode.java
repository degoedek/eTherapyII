package com.example.etherapyii;

class QuaternionNode {
    double q0, q1, q2, q3;
    QuaternionNode next;
    QuaternionNode prev;

    public QuaternionNode(double q0, double q1, double q2, double q3) {
        this.q0 = q0;
        this.q1 = q1;
        this.q2 = q2;
        this.q3 = q3;
        this.next = null;
        this.prev = null;
    }
}