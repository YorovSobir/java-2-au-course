package ru.spbau.mit.java2;

public class LightExecutionException extends RuntimeException {
    public LightExecutionException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
