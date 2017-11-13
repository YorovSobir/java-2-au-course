package ru.spbau.mit.java2.api;

import java.io.DataInputStream;

public interface FTPClient {
    void connect();
    void disconnect();
    DataInputStream get(String path);
    DataInputStream list(String path);
}
