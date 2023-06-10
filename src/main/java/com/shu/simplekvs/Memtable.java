package com.shu.simplekvs;

import java.util.TreeMap;

public class Memtable extends TreeMap<String,String>{
    private int sizeLimit;
    
    public Memtable(int limit){
        this.sizeLimit = limit;
    }

    public Memtable() {
        this(1024);
    }

    private boolean isOverLimit() {
        return this.sizeLimit <= this.size();
    }

    public static void main(String[] args) {
        Memtable mt = new Memtable(100);
        boolean res = mt.isOverLimit();
        System.out.println(res);
    }
}
