package ru.spbau.mit.tracker.api;

public interface TorrentTracker extends AutoCloseable {
    void start();
    void stop();
}
