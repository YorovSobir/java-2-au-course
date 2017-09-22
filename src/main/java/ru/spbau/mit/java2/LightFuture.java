package ru.spbau.mit.java2;

import ru.spbau.mit.java2.api.ThreadPool;

import java.util.function.Function;
import java.util.function.Supplier;

public class LightFuture<X> implements Runnable {
    private Supplier<X> supplier;
    private ThreadPool threadPool;
    private X value;
    private Throwable throwable;
    private volatile boolean done = false;

    public LightFuture(ThreadPool threadPool, Supplier<X> supplier) {
        this.threadPool = threadPool;
        this.supplier = supplier;
    }

    @Override
    public synchronized void run() {
        try {
            value = supplier.get();
        } catch (Throwable e) {
            throwable = e;
        } finally {
            done = true;
            this.notifyAll();
        }
    }

    public X get() {
        synchronized (this) {
            while (!done) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new LightExecutionException("get exception while waiting", e);
                }
            }
        }
        if (throwable != null) {
            throw new LightExecutionException("supplier throw exception", throwable);
        }
        return value;
    }

    public boolean isReady() {
        return done;
    }

    public <Y> LightFuture<Y> thenApply(Function<X, Y> function) {
        Supplier<Y> supplier = () -> function.apply(LightFuture.this.get());
        return threadPool.submit(supplier);
    }
}
