package ru.spbau.mit.java2;

import ru.spbau.mit.java2.api.ThreadPool;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

public final class ThreadPoolImpl implements ThreadPool {

    private Queue<Runnable> taskQueue = new LinkedList<>();
    private List<Worker> workers = new ArrayList<>();
    public ThreadPoolImpl(int noOfThreads) {
        for (int i = 0; i < noOfThreads; ++i) {
            workers.add(new Worker());
        }
        for (int i = 0; i < workers.size(); ++i) {
            workers.get(i).setName("threadPool: thread-" + i);
            workers.get(i).start();
        }
    }

    @Override
    public <T> LightFuture<T> submit(Supplier<T> supplier) {
        LightFuture<T> future = new LightFuture<>(this, supplier);
        synchronized (taskQueue) {
            taskQueue.add(future);
            taskQueue.notifyAll();
        }
        return future;
    }

    @Override
    public int activeThread() {
        return workers.size();
    }

    @Override
    public synchronized void shutdown() {
        workers.forEach(Thread::interrupt);
        workers.forEach(w -> {
            try {
                w.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("interrupted when shutdown", e);
            }
        });
    }

    private final class Worker extends Thread {

        @Override
        public void run() {
            Runnable task;
            while (!isInterrupted()) {
                synchronized (taskQueue) {
                    while (taskQueue.isEmpty()) {
                        try {
                            taskQueue.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    task = taskQueue.remove();
                }
                task.run();
            }
        }
    }
}
