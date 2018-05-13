package ru.spbau.mit.tracker.response;

import ru.spbau.mit.data.ClientInfo;
import ru.spbau.mit.data.FileInfo;
import ru.spbau.mit.utils.BEncoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SourcesResponse implements TrackerResponse {
    private int size;
    private List<ClientInfo> clients;
    private final BEncoder bEncoder = new BEncoder();


    public SourcesResponse(List<ClientInfo> clients) {
        size = clients.size();
        this.clients = clients;
    }

    public SourcesResponse(String bEncode) {
        bEncoder.setInput(bEncode);
        clients = new ArrayList<>();
        ArrayList<Object> objects = bEncoder.readAll();
        this.size = (int) objects.get(0);
        objects = (ArrayList<Object>) objects.get(1);
        for (Object map : objects) {
            ArrayList<Object> ip = (ArrayList<Object>) ((Map<String, Object>) map).get("ip");
            byte[] clientIp = new byte[ip.size()];
            for (int i = 0; i < ip.size(); i++) {
                clientIp[i] = (byte) ((int)ip.get(i));
            }
            int clientPort = (int) ((Map<String, Object>) map).get("port");
            clients.add(new ClientInfo(clientIp, clientPort));
        }
    }

    public List<ClientInfo> getClients() {
        return clients;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String getString() {
        bEncoder.resetOutput();
        bEncoder.writeAll(size, clients);
        return bEncoder.getString();
    }
}
