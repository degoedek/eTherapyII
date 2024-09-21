package com.example.etherapyii;

public class GenericNode<T> {
    T data;
    long time = System.currentTimeMillis();
    GenericNode<T> next;
    GenericNode<T> prev;

    public GenericNode(T data) {
        this.data = data;
        this.next = null;
        this.prev = null;
    }
}
