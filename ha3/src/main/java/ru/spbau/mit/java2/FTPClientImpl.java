package ru.spbau.mit.java2;

import ru.spbau.mit.java2.api.FTPClient;

import java.io.*;
import java.net.Socket;

public class FTPClientImpl implements FTPClient {

    private static final int BUF_SIZE = 256;
    private static final int LIST_REQUEST = 1;
    private static final int GET_REQUEST = 2;

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private String host;
    private int port;

    public FTPClientImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void connect() {
        try {
            socket = new Socket(host, port);
        } catch (IOException e) {
            throw new IllegalStateException("Could not connect to server: " + host + " in port " + port, e);
        }
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void disconnect() {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not stopped client", e);
        }
    }

    @Override
    public DataInputStream get(String path) {
        try {
            dos.writeInt(GET_REQUEST);
            dos.writeUTF(path);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            long size = dis.readLong();
            out.writeLong(size);

            byte[] buff = new byte[BUF_SIZE];
            long readSize = 0;
            while (readSize < size) {
                int count = dis.read(buff, 0, BUF_SIZE);
                readSize += count;
                out.write(buff, 0, count);
            }
            return new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public DataInputStream list(String path) {
        try {
            dos.writeInt(LIST_REQUEST);
            dos.writeUTF(path);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            long size = dis.readLong();
            out.writeLong(size);
            for (int i = 0; i < size; i++) {
                out.writeUTF(dis.readUTF());
                out.writeBoolean(dis.readBoolean());
            }

            return new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
