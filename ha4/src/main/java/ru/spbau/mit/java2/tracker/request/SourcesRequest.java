package ru.spbau.mit.java2.tracker.request;

import ru.spbau.mit.java2.common.api.Request;
import ru.spbau.mit.java2.common.api.RequestConfig;

import java.io.Serializable;

public class SourcesRequest implements Request, Serializable {
    private int fileId;

    public SourcesRequest(int fileId) {
        this.fileId = fileId;
    }

    public int getFileId() {
        return fileId;
    }

    @Override
    public byte getType() {
        return RequestConfig.SOURCES_REQUEST;
    }
}
