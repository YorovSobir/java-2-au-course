package ru.spbau.mit.tracker.request;

import ru.spbau.mit.common.api.Request;
import ru.spbau.mit.common.api.RequestConfig;
import ru.spbau.mit.tracker.response.SourcesResponse;

import java.io.Serializable;

public class SourcesRequest implements Request, TrackerRequest {
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
