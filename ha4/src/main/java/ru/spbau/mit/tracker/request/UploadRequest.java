package ru.spbau.mit.tracker.request;

import ru.spbau.mit.common.api.Request;
import ru.spbau.mit.common.api.RequestConfig;

import java.io.Serializable;

public class UploadRequest implements Request, TrackerRequest {
    private String name;
    private long size;

    public UploadRequest(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    @Override
    public byte getType() {
        return RequestConfig.UPLOAD_REQUEST;
    }
}
