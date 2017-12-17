package ru.spbau.mit.java2.tracker.api;

public interface TorrentTracker extends AutoCloseable {
    void start();
    void stop();
}
