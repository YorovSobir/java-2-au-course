package ru.spbau.mit.data;

import java.io.Serializable;
import java.util.Arrays;

public class ClientInfo implements Serializable {
    private byte[] clientIp;
    private int clientPort;

    public ClientInfo(byte[] clientIp, int clientPort) {
        this.clientIp = clientIp;
        this.clientPort = clientPort;
    }

    public byte[] getClientIp() {
        return clientIp;
    }

    public int getClientPort() {
        return clientPort;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(clientIp);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ClientInfo) {
            ClientInfo otherClientInfo = (ClientInfo) other;
            return Arrays.equals(clientIp, otherClientInfo.clientIp)
                    && clientPort == otherClientInfo.clientPort;
        }
        return other.equals(this);
    }
}
