package ru.spbau.mit.java2.api;

import ru.spbau.mit.java2.LightFuture;

import java.util.function.Supplier;

public interface ThreadPool {
    <T> LightFuture<T> submit(Supplier<T> supplier);
    boolean isShutdown();
    void shutdown();

}
