package com.example.etherapyii;


public class DoublyLinkedList {
    private QuaternionNode head;

    public DoublyLinkedList() {
        head = new QuaternionNode(new Quaternion(0,0,0,0));
        head.next = head;
        head.prev = head;
    }

    public void insert(Quaternion t) {
        if(t == null){
            return;
        }
        QuaternionNode newNode = new QuaternionNode(t);
        newNode.prev = head;
        newNode.next = head.next;
        head.next.prev = newNode;
        head.next = newNode;
    }

    // Method to average the quaternions in the list
    public Quaternion averageQuaternions() {
        if (head.next == head) {
            //list is empty


        }

        int count = 0;
        float sumQ0 = 0.0F;
        float sumQ1 = 0.0F;
        float sumQ2 = 0.0F;
        float sumQ3 = 0.0F;

        QuaternionNode temp = head.next;
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








