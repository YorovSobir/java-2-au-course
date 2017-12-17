package ru.spbau.mit.java2.tracker.response;

import ru.spbau.mit.java2.data.ClientInfo;

import java.io.Serializable;
import java.util.List;

public class SourcesResponse implements Serializable {
    private int size;
    private List<ClientInfo> clients;

    public SourcesResponse(List<ClientInfo> clients) {
        size = clients.size();
        this.clients = clients;
    }

    public List<ClientInfo> getClients() {
        return clients;
    }

    public int getSize() {
        return size;
    }
}
