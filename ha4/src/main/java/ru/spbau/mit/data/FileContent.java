package ru.spbau.mit.data;

import java.io.Serializable;

public class FileContent implements Serializable {
    private byte[] content;

    public FileContent(byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }
}
