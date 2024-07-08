package com.example.etherapyii;


class QuaternionNode {
    double q0, q1, q2, q3;
    QuaternionNode next;
    QuaternionNode prev;

    public QuaternionNode(Quaternion q) {
        if(q == null){
            return;
        }
        this.q0 = q.w();
        this.q1 = q.x();
        this.q2 = q.y();
        this.q3 = q.z();
        this.next = null;
        this.prev = null;
    }
}