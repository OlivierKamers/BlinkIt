package com.choosemuse.example.libmuse;

import java.util.LinkedList;

public class MovingAverage {
    LinkedList<Double> queue;
    int size;
    double avg;

    /** Initialize your data structure here. */
    public MovingAverage() {
        this.queue = new LinkedList<Double>();
        this.size = 20;
    }

    public void next(double val) {
        if(queue.size()<this.size){
            queue.offer(val);
            int sum=0;
            for(double i: queue){
                sum+=i;
            }
            avg = (double)sum/queue.size();
        }else{
            double head = queue.poll();
            double minus = (double)head/this.size;
            queue.offer(val);
            double add = (double)val/this.size;
            avg = avg + add - minus;
        }
    }

    public double get() {
        return avg;
    }
}
