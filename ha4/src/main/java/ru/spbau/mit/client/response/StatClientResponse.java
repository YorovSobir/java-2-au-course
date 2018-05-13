package ru.spbau.mit.client.response;

import java.io.Serializable;
import java.util.List;

public class StatClientResponse implements Serializable {
    private int count;
    private List<Integer> fileParts;

    public StatClientResponse(List<Integer> fileParts) {
        count = fileParts.size();
        this.fileParts = fileParts;
    }

    public int getCount() {
        return count;
    }

    public List<Integer> getFileParts() {
        return fileParts;
    }
}
