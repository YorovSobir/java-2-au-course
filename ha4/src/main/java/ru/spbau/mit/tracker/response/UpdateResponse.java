package ru.spbau.mit.tracker.response;

import ru.spbau.mit.utils.BEncoder;

import java.io.Serializable;

public class UpdateResponse implements TrackerResponse {
    private boolean status;
    private final BEncoder bEncoder = new BEncoder();

    public UpdateResponse(String bEncode) {
        bEncoder.setInput(bEncode);
        if ((int) bEncoder.read() == 1) {
            status = true;
        } else {
            status = false;
        }
    }

    public UpdateResponse(boolean status) {
        this.status = status;
    }

    public boolean isStatus() {
        return status;
    }

    @Override
    public String getString() {
        bEncoder.resetOutput();
        bEncoder.write(status);
        return bEncoder.getString();
    }
}
