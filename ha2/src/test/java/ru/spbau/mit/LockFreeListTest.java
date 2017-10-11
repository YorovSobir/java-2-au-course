package ru.spbau.mit;

import org.junit.Assert;
import org.junit.Test;
import ru.spbau.mit.java2.LockFreeListImpl;
import ru.spbau.mit.java2.api.LockFreeList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LockFreeListTest {

    static final int THREADS_COUNT = 100;

    private static class ElemAppended {
        private Integer elem;
        private boolean pushed = false;
        private boolean removed = false;

        ElemAppended(Integer elem) {
            this.elem = elem;
        }
    }

    private static void executeThread(List<Thread> threads) {
        threads.forEach(Thread::start);
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("join failed", e);
            }
        });
    }

    private static LockFreeList<Integer> fillList() {
        final LockFreeList<Integer> list = new LockFreeListImpl<>();
        final CyclicBarrier barier = new CyclicBarrier(THREADS_COUNT);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS_COUNT; ++i) {
            int finalI = i;
            threads.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                list.append(finalI);
            }));
        }
        executeThread(threads);
        return list;
    }

    @Test
    public void testAdd() {
        LockFreeList<Integer> list = fillList();

        for (int i = 0; i < THREADS_COUNT; ++i) {
            Assert.assertTrue(list.contains(i));
        }
    }

    @Test
    public void testRemove() {
        LockFreeList<Integer> list = fillList();
        CyclicBarrier barier = new CyclicBarrier(THREADS_COUNT);
        List<Thread> threads = new ArrayList<>();
        final boolean[] result = {true};
        for (int i = 0; i < THREADS_COUNT; ++i) {
            Integer finalI = i;
            threads.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                if (!list.remove(finalI)) {
                    result[0] = false;
                }
            }));
        }
        executeThread(threads);

        Assert.assertTrue(list.isEmpty());
        Assert.assertTrue(result[0]);

        threads = new ArrayList<>();
        for (int i = 0; i < THREADS_COUNT; ++i) {
            Integer finalI = i;
            threads.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                try {
                    if (list.remove(finalI)) {
                        result[0] = false;
                    }
                } catch (AssertionError e) {
                    throw e;
                }
            }));
        }
        executeThread(threads);
        Assert.assertTrue(result[0]);
    }

    @Test
    public void testContains() {
        LockFreeList<Integer> list = fillList();
        CyclicBarrier barier = new CyclicBarrier(THREADS_COUNT);
        List<Thread> threads = new ArrayList<>();
        final boolean[] result = {true};
        for (int i = 0; i < THREADS_COUNT; ++i) {
            int finalI = i;
            threads.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                if (!list.contains(finalI)) {
                    result[0] = false;
                }
            }));
        }
        executeThread(threads);
        Assert.assertTrue(result[0]);
    }

    @Test
    public void testMixRemoveAdd() {
        LockFreeList<Integer> list = new LockFreeListImpl<>();
        List<Thread> addRunner = new ArrayList<>();
        List<Thread> removeRunner = new ArrayList<>();
        CyclicBarrier barier = new CyclicBarrier(THREADS_COUNT);
        final boolean[] result = {true};

        for (int i = 0; i < THREADS_COUNT / 2; ++i) {
            final ElemAppended elem = new ElemAppended(i);
            addRunner.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                synchronized (elem) {
                    list.append(elem.elem);
                    elem.pushed = true;
                    elem.notify();
                }
            }));

            removeRunner.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                synchronized (elem) {
                    while (!elem.pushed) {
                        try {
                            elem.wait();
                        } catch (InterruptedException e) { }
                    }
                    if (!list.remove(elem.elem)) {
                        result[0] = false;
                    }
                }
            }));
        }

        List<Thread> allThreads = new ArrayList<>(addRunner);
        allThreads.addAll(removeRunner);
        executeThread(allThreads);

        Assert.assertTrue(list.isEmpty());
        Assert.assertTrue(result[0]);
    }

    @Test
    public void testMixAddContains() {
        LockFreeList<Integer> list = new LockFreeListImpl<>();
        List<Thread> addRunner = new ArrayList<>();
        List<Thread> containRunner = new ArrayList<>();
        CyclicBarrier barier = new CyclicBarrier(THREADS_COUNT);
        final boolean[] result = {true};

        for (int i = 0; i < THREADS_COUNT / 2; ++i) {
            final ElemAppended elem = new ElemAppended(i);
            addRunner.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                synchronized (elem) {
                    list.append(elem.elem);
                    elem.pushed = true;
                    elem.notify();
                }
            }));

            containRunner.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                synchronized (elem) {
                    while (!elem.pushed) {
                        try {
                            elem.wait();
                        } catch (InterruptedException e) { }
                    }
                    if (!list.contains(elem.elem)) {
                        result[0] = false;
                    }
                }
            }));
        }

        List<Thread> allThreads = new ArrayList<>(addRunner);
        allThreads.addAll(containRunner);
        executeThread(allThreads);

        Assert.assertTrue(result[0]);
    }

    @Test
    public void testMixAll() {
        LockFreeList<Integer> list = new LockFreeListImpl<>();
        List<Thread> addRunner = new ArrayList<>();
        List<Thread> removeRunner = new ArrayList<>();
        List<Thread> containRunner = new ArrayList<>();
        final int runnerCount = 3;
        CyclicBarrier barier = new CyclicBarrier(THREADS_COUNT - (THREADS_COUNT % runnerCount == 0 ? 0 : 1));
        final boolean[] result = {true};

        for (int i = 0; i < THREADS_COUNT / runnerCount; ++i) {
            final ElemAppended elem = new ElemAppended(i);
            addRunner.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                synchronized (elem) {
                    list.append(elem.elem);
                    elem.pushed = true;
                    elem.notifyAll();
                }
            }));

            removeRunner.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                synchronized (elem) {
                    while (!elem.pushed) {
                        try {
                            elem.wait();
                        } catch (InterruptedException e) { }
                    }
                    if (!list.remove(elem.elem)) {
                        result[0] = false;
                        return;
                    }
                    elem.removed = true;
                }
            }));

            containRunner.add(new Thread(() -> {
                try {
                    barier.await();
                } catch (Throwable t) { }
                synchronized (elem) {
                    while (!elem.pushed) {
                        try {
                            elem.wait();
                        } catch (InterruptedException e) { }
                    }
                    if (!list.contains(elem.elem)) {
                        if (!elem.removed) {
                            result[0] = false;
                        }
                    } else {
                        if (elem.removed) {
                            result[0] = false;
                        }
                    }
                }
            }));
        }

        List<Thread> allThreads = new ArrayList<>(addRunner);
        allThreads.addAll(removeRunner);
        allThreads.addAll(containRunner);
        executeThread(allThreads);

        Assert.assertTrue(result[0]);
    }
}
