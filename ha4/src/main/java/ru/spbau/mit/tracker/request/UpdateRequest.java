package ru.spbau.mit.tracker.request;

import ru.spbau.mit.common.api.Request;
import ru.spbau.mit.common.api.RequestConfig;
import ru.spbau.mit.data.ClientDataInfo;

import java.io.Serializable;

public class UpdateRequest implements Request, TrackerRequest {
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
