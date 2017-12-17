package ru.spbau.mit.java2.tracker.response;

import java.io.Serializable;

public class UpdateResponse implements Serializable {
    private boolean status;

    public UpdateResponse(boolean status) {
        this.status = status;
    }

    public boolean isStatus() {
        return status;
    }
}
