package ru.spbau.mit.java2;

import java.util.function.Function;

public interface LightFuture<X> {
    X get();
    boolean isReady();
    <Y> LightFuture<Y> thenApply(Function<X, Y> function);
}
