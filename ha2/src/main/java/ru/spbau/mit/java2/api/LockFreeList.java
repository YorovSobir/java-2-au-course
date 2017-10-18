package ru.spbau.mit.java2.api;

public interface LockFreeList<E> {
    void append(E elem);
    boolean remove(E elem);
    boolean contains(E elem);
    boolean isEmpty();
}
