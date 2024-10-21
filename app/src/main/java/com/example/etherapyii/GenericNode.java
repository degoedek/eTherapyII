package com.example.etherapyii;

public class GenericNode<T> {
    T data;
    Quaternion s1Q, s2Q;
    int repNum;
    Boolean acquired;
    long time = System.currentTimeMillis();
    GenericNode<T> next;
    GenericNode<T> prev;

    public GenericNode(T data, Quaternion s1Q, Quaternion s2Q, int repNum, Boolean acquired) {
        this.data = data;
        this.s1Q = s1Q;
        this.s2Q = s2Q;
        this.repNum = repNum;
        this.acquired = acquired;
        this.next = null;
        this.prev = null;
    }
}
