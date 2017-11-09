package ru.spbau.mit.java2;

import ru.spbau.mit.java2.api.ThreadPool;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ThreadPoolImpl implements ThreadPool {

    private volatile boolean shutdown = false;
    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private final List<Worker> workers = new ArrayList<>();
    public ThreadPoolImpl(int noOfThreads) {
        for (int i = 0; i < noOfThreads; ++i) {
            Worker worker = new Worker("threadPool: thread-" + i);
            worker.start();
            workers.add(worker);
        }
    }

    private <T> void addFuture(LightFutureImpl<T> future) {
        synchronized (taskQueue) {
            taskQueue.add(future::run);
            taskQueue.notify();
        }
    }

    @Override
    public synchronized  <T> LightFuture<T> submit(Supplier<T> supplier) {
        if (isShutdown()) {
            throw new RuntimeException("there are not running workers");
        }

        LightFutureImpl<T> future = new LightFutureImpl<>(supplier);
        addFuture(future);
        return future;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
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
        workers.clear();
        shutdown = true;
    }

    private final class Worker extends Thread {

        Worker(String name) {
            super(name);
        }

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

    private final class LightFutureImpl<X> implements LightFuture<X> {
        private final Supplier<X> supplier;
        private X value;
        private Throwable throwable;
        private volatile boolean done = false;
        private final Queue<LightFutureImpl<?>> dependentFutureQueue = new LinkedList<>();


        LightFutureImpl(Supplier<X> supplier) {
            this.supplier = supplier;
        }

        private void run() {
            setValue();
            submitDependentFuture();
        }

        private void setValue() {
            if (!done) {
                synchronized (this) {
                    if (!done) {
                        try {
                            value = supplier.get();
                        } catch (Throwable e) {
                            throwable = e;
                        } finally {
                            done = true;
                            notifyAll();
                        }
                    }
                }
            }
        }

        private void submitDependentFuture() {
            synchronized (dependentFutureQueue) {
                dependentFutureQueue.forEach(ThreadPoolImpl.this::addFuture);
                dependentFutureQueue.clear();
            }
        }

        @Override
        public X get() {
            setValue();
            if (throwable != null) {
                throw new LightExecutionException("supplier throws exception", throwable);
            }
            return value;
        }

        @Override
        public boolean isReady() {
            return done;
        }

        @Override
        public <Y> LightFuture<Y> thenApply(Function<X, Y> function) {
            Supplier<Y> supplier = () -> function.apply(LightFutureImpl.this.get());
            if (isReady()) {
                return ThreadPoolImpl.this.submit(supplier);
            } else {
                return submitDependentFuture(supplier);
            }
        }

        private <Y> LightFuture<Y> submitDependentFuture(Supplier<Y> supplier) {
            LightFutureImpl<Y> dependentFuture = new LightFutureImpl<>(supplier);
            synchronized (dependentFutureQueue) {
                dependentFutureQueue.add(dependentFuture);
            }
            return dependentFuture;
        }
    }
}
