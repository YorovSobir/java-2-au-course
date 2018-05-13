package ru.spbau.mit.data;

import java.io.Serializable;
import java.util.List;

public class ClientDataInfo implements Serializable {
    private int clientPort;
    private int count;
    private List<Integer> filesId;

    public ClientDataInfo(int clientPort, List<Integer> filesId) {
        this.clientPort = clientPort;
        count = filesId.size();
        this.filesId = filesId;
    }

    public int getClientPort() {
        return clientPort;
    }

    public int getCount() {
        return count;
    }

    public List<Integer> getFilesId() {
        return filesId;
    }

}
