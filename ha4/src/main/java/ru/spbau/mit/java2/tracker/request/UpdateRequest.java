package ru.spbau.mit.java2.tracker.request;

import ru.spbau.mit.java2.common.api.Request;
import ru.spbau.mit.java2.common.api.RequestConfig;
import ru.spbau.mit.java2.data.ClientDataInfo;

import java.io.Serializable;

public class UpdateRequest implements Request, Serializable {
    private ClientDataInfo clientDataInfo;

    public UpdateRequest(ClientDataInfo clientDataInfo) {
        this.clientDataInfo = clientDataInfo;
    }

    public ClientDataInfo getClientDataInfo() {
        return clientDataInfo;
    }

    @Override
    public byte getType() {
        return RequestConfig.UPDATE_REQUEST;
    }

}
