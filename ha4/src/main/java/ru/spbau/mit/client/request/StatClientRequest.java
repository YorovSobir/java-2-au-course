package ru.spbau.mit.client.request;

import ru.spbau.mit.common.api.Request;
import ru.spbau.mit.common.api.RequestConfig;

import java.io.Serializable;

public class StatClientRequest implements Request, Serializable {
    private int fileId;

    public StatClientRequest(int fileId) {
        this.fileId = fileId;
    }

    @Override
    public byte getType() {
        return RequestConfig.STAT_REQUEST;
    }

    public int getFileId() {
        return fileId;
    }
}
