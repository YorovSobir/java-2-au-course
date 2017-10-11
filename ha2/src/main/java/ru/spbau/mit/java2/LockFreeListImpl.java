package ru.spbau.mit.java2;

import ru.spbau.mit.java2.api.LockFreeList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeListImpl<E> implements LockFreeList<E> {

    private static class Node<E> {
        private E elem;
        private int hash;
        private Type type;
        private AtomicMarkableReference<Node<E>> next;

        Node(E elem, int hash, Type type, Node<E> next) {
            this.elem = elem;
            this.hash = hash;
            this.type = type;
            this.next = new AtomicMarkableReference<>(next, false);
        }
    }

    private static class Positions<E> {
        private Node<E> pred;
        private Node<E> curr;

        Positions(Node<E> pred, Node<E> curr) {
            this.pred = pred;
            this.curr = curr;
        }
    }

    private enum Type {
        HEAD,
        NODE,
        TAIL
    }

    private Node<E> tail;
    private AtomicInteger size = new AtomicInteger(0);

    public LockFreeListImpl() {
        Node<E> head = new Node<>(null, -1, Type.HEAD, null);
        tail = new Node<>(null, 1, Type.TAIL, head);
    }


    @Override
    public void append(E elem) {
        while (true) {
            // I want to append element "fast", for this reason i save (traverse) list in reverse order
            Node<E> last = tail.next.getReference();
            Node<E> newNode = new Node<>(elem, elem.hashCode(), Type.NODE, last);
            if (tail.next.compareAndSet(last, newNode, false, false)) {
                size.getAndIncrement();
                break;
            }
        }
    }

    private Positions<E> find(Type type, int hash) {
        boolean[] marked = {false};
        Node<E> curr;
        Node<E> pred;
        Node<E> succ;
        boolean snip;
        while (true) {
            pred = tail;
            curr = tail.next.getReference();
            while (curr != null) {
                succ = curr.next.get(marked);
                while (marked[0]) {
                    snip = pred.next.compareAndSet(curr, succ, false, false);
                    if (!snip) {
                        break;
                    }
                    if (succ == null) {
                        return null;
                    }
                    curr = succ;
                    succ = curr.next.get(marked);
                }
                if (marked[0]) {
                    break;
                }
                if (curr.type == type && curr.hash == hash) {
                    return new Positions<>(pred, curr);
                }
                pred = curr;
                curr = succ;
            }
            if (curr == null) {
                return null;
            }
        }
    }

    @Override
    public boolean remove(E elem) {
        while (true) {
            Positions<E> pos = find(Type.NODE, elem.hashCode());
            if (pos == null) {
                return false;
            }

            Node<E> succ = pos.curr.next.getReference();
            if (pos.curr.next.attemptMark(succ, true)) {
                size.getAndDecrement();
                pos.pred.next.compareAndSet(pos.curr, succ, false, false);
                return true;
            }
        }
    }

    @Override
    public boolean contains(E elem) {
        return find(Type.NODE, elem.hashCode()) != null;
    }

    @Override
    public boolean isEmpty() {
        return size.get() == 0;
    }
}
