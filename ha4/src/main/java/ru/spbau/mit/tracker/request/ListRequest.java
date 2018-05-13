package ru.spbau.mit.tracker.request;

import ru.spbau.mit.common.api.Request;
import ru.spbau.mit.common.api.RequestConfig;
import ru.spbau.mit.tracker.response.TrackerResponse;

import java.io.Serializable;

public class ListRequest implements Request, TrackerRequest {

    @Override
    public byte getType() {
        return RequestConfig.LIST_REQUEST;
    }
}
