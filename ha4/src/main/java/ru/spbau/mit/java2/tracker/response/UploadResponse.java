package ru.spbau.mit.java2.tracker.response;

import java.io.Serializable;

public class UploadResponse implements Serializable {
    private int fileId;

    public UploadResponse(int fileId) {
        this.fileId = fileId;
    }

    public int getFileId() {
        return fileId;
    }
}
