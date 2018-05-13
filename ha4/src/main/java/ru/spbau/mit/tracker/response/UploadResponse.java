package ru.spbau.mit.tracker.response;

import ru.spbau.mit.utils.BEncoder;

import java.io.Serializable;

public class UploadResponse implements TrackerResponse {
    private int fileId;
    private final BEncoder bEncoder = new BEncoder();

    public UploadResponse(String bEncode) {
        bEncoder.setInput(bEncode);
        fileId = (int) bEncoder.read();
    }

    public UploadResponse(int fileId) {
        this.fileId = fileId;
    }

    public int getFileId() {
        return fileId;
    }

    @Override
    public String getString() {
        bEncoder.resetOutput();
        bEncoder.write(fileId);
        return bEncoder.getString();
    }
}
