package com.linearity.feedhelper.client.utils;

import java.util.concurrent.ConcurrentLinkedDeque;

public class LockFreeEvictingDeque<E> extends ConcurrentLinkedDeque<E> {
    private final int maxSize;

    public LockFreeEvictingDeque(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        this.maxSize = maxSize;
    }

    @Override
    public void addLast(E e) {
        super.addLast(e);
        evictIfNeededFirst();
    }

    @Override
    public void addFirst(E e) {
        super.addFirst(e);
        evictIfNeededLast();
    }

    private void evictIfNeededFirst() {
        while (super.size() > maxSize) {
            super.pollFirst();
        }
    }

    private void evictIfNeededLast() {
        while (super.size() > maxSize) {
            super.pollLast();
        }
    }
}