package ru.spbau.mit.java2.client.request;

import ru.spbau.mit.java2.common.api.Request;
import ru.spbau.mit.java2.common.api.RequestConfig;

import java.io.Serializable;

public class GetClientRequest implements Request, Serializable {
    private int fileId;
    private int filePart;

    public GetClientRequest(int fileId, int filePart) {
        this.fileId = fileId;
        this.filePart = filePart;
    }

    @Override
    public byte getType() {
        return RequestConfig.GET_REQUEST;
    }

    public int getFileId() {
        return fileId;
    }

    public int getFilePart() {
        return filePart;
    }
}
