package com.example.etherapyii;

import com.mbientlab.metawear.data.Quaternion;
public class DoublyLinkedList {
    private QuaternionNode head;

    public DoublyLinkedList() {
        this.head = null;
    }

    // Method to insert a quaternion at the beginning of the list
    public void insert(float q0, float q1, float q2, float q3) {
        QuaternionNode newNode = new QuaternionNode(q0, q1, q2, q3);
        if (head == null) {
            head = newNode;
            head.next = head;
            head.prev = head;
        } else {
            QuaternionNode last = head.prev;
            last.next = newNode;
            newNode.prev = last;
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }
    }

    // Method to average the quaternions in the list
    public Quaternion averageQuaternions() {
        if (head == null) {
            //list is empty


        }

        int count = 0;
        float sumQ0 = 0.0F;
        float sumQ1 = 0.0F;
        float sumQ2 = 0.0F;
        float sumQ3 = 0.0F;

        QuaternionNode temp = head;
        do {
            sumQ0 += (float) temp.q0;
            sumQ1 += (float) temp.q1;
            sumQ2 += (float) temp.q2;
            sumQ3 += (float) temp.q3;
            count++;
            temp = temp.next;
        } while (temp != head);

        // Calculate the average quaternion
        float avgQ0 = sumQ0 / count;
        float avgQ1 = sumQ1 / count;
        float avgQ2 = sumQ2 / count;
        float avgQ3 = sumQ3 / count;

        // Normalize the average quaternion
        double magnitude = Math.sqrt(avgQ0 * avgQ0 + avgQ1 * avgQ1 + avgQ2 * avgQ2 + avgQ3 * avgQ3);
        avgQ0 /= (float) magnitude;
        avgQ1 /= (float) magnitude;
        avgQ2 /= (float) magnitude;
        avgQ3 /= (float) magnitude;

        return new Quaternion(avgQ0, avgQ1, avgQ2, avgQ3);
    }

}