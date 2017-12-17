package ru.spbau.mit.java2.tracker.request;

import ru.spbau.mit.java2.common.api.Request;
import ru.spbau.mit.java2.common.api.RequestConfig;

import java.io.Serializable;

public class ListRequest implements Request, Serializable {

    @Override
    public byte getType() {
        return RequestConfig.LIST_REQUEST;
    }
}
