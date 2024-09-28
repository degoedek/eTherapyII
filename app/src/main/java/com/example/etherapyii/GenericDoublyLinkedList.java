package com.example.etherapyii;

public class GenericDoublyLinkedList<T> {
    private GenericNode<T> head;
    private int size;

    public GenericDoublyLinkedList() {
        head = new GenericNode<>(null, null, null, 0, null);
        head.next = head;
        head.prev = head;
        size = 0;
    }

    public void insert(T data, Quaternion s1Q, Quaternion s2Q, int repNum, Boolean acquired) {
        if(data == null){
            return;
        }
        GenericNode<T> newNode = new GenericNode<>(data, s1Q, s2Q, repNum, acquired);
        newNode.next = head;
        newNode.prev = head.prev;
        head.prev.next = newNode;
        head.prev = newNode;
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
