package com.example.etherapyii;

public class GenericDoublyLinkedList<T> {
    private GenericNode<T> head;
    private int size;

    public GenericDoublyLinkedList() {
        head = new GenericNode<>(null);
        head.next = head;
        head.prev = head;
        size = 0;
    }

    public void insert(T data) {
        if(data == null){
            return;
        }
        GenericNode<T> newNode = new GenericNode<>(data);
        newNode.prev = head;
        newNode.next = head.next;
        head.next.prev = newNode;
        head.next = newNode;
        size++;
    }

    public boolean isEmpty() {return size == 0;}

    public GenericNode<T> removeFront() {
        if (isEmpty()) {
            return null;
        }

        GenericNode<T> firstNode = head.next;
        head.next = firstNode.next;
        firstNode.next.prev = head;

        size--;
        return firstNode;
    }
}
