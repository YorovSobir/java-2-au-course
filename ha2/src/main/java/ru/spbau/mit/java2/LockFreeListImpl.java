package ru.spbau.mit.java2;

import ru.spbau.mit.java2.api.LockFreeList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeListImpl<E> implements LockFreeList<E> {

    private static class Node<E> {
        private E elem;
        private NodeType nodeType;
        private AtomicMarkableReference<Node<E>> next;

        Node(E elem, NodeType nodeType, Node<E> next) {
            this.elem = elem;
            this.nodeType = nodeType;
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

    private enum NodeType {
        HEAD,
        NODE,
        TAIL
    }

    private Node<E> tail = new Node<>(null, NodeType.TAIL, null);
    private Node<E> head = new Node<>(null, NodeType.HEAD, tail);
    private Node<E> predTail = head;
    private AtomicInteger size = new AtomicInteger(0);

    private static <E> boolean compare(Node<E> cur, Node<E> elem) {
        if (elem.nodeType == NodeType.TAIL || elem.nodeType == NodeType.HEAD) {
            return cur.nodeType == elem.nodeType;
        }
        return cur.nodeType == elem.nodeType && cur.elem == elem.elem;
    }

    @Override
    public void append(E elem) {
        boolean[] marked = {false};
        Node<E> newNode = new Node<>(elem, NodeType.NODE, tail);
        while (true) {
            Node<E> curTail = predTail.next.get(marked);
            if (curTail != tail || marked[0]) {
                Positions<E> tailPos = find(tail);
                if (tailPos == null) {
                    throw new RuntimeException("it's unreal");
                }
                predTail = tailPos.pred;
            }
            if (predTail.next.compareAndSet(tail, newNode, false, false)) {
                size.getAndIncrement();
                predTail = newNode;
                break;
            }
        }
    }

    private Positions<E> find(Node<E> elem) {
        boolean[] marked = {false};
        Node<E> curr;
        Node<E> pred;
        Node<E> succ;
        boolean snip;
        while (true) {
            pred = head;
            curr = head.next.getReference();
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
                if (compare(curr, elem)) {
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
        Node<E> temp = new Node<>(elem, NodeType.NODE, null);
        while (true) {
            Positions<E> pos = find(temp);
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
        return find(new Node<>(elem, NodeType.NODE, null)) != null;
    }

    @Override
    public boolean isEmpty() {
        return size.get() == 0;
    }
}
