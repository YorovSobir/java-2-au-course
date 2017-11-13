package ru.spbau.mit.java2;

import ru.spbau.mit.java2.api.FTPServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class FTPServerImpl implements FTPServer {

    private static final int THREADS_COUNT = 8;

    private ExecutorService executorService = Executors.newFixedThreadPool(THREADS_COUNT);
    private int port;
    private ServerSocket serverSocket;

    public FTPServerImpl(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            serverSocket = server;
            while (true) {
                Socket client = serverSocket.accept();
                executorService.submit(new ClientHandle(client));
            }
        } catch (SocketException e) {
            // socket closed
        } catch (IOException e) {
            throw new IllegalStateException("Could not listen on port: " + port, e);
        }
    }

    @Override
    public void stop() {
        if (serverSocket == null) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    private static final class ClientHandle implements Runnable {

        private static final int BUF_SIZE = 512;
        private Socket client;
        private DataOutputStream out;

        private ClientHandle(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try (DataInputStream din = new DataInputStream(client.getInputStream());
                    DataOutputStream dout = new DataOutputStream(client.getOutputStream())) {
                out = dout;
                boolean end = false;
                while (!end) {
                    int operation = din.readInt();

                    switch (operation) {
                        case 1:  list(din.readUTF());
                                 break;
                        case 2:  get(din.readUTF());
                                 break;
                        default: end = true;
                                 break;
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("cannot open client socket's stream", e);
            }
        }

        private void list(String dirPath) {
            long size = 0;
            List<Path> files = new ArrayList<>();
            Path path = Paths.get(dirPath);
            if (Files.exists(path)) {
                try {
                    files = Files.list(path).collect(Collectors.toList());
                    size = files.size();
                } catch (IOException e) {
                    throw new IllegalStateException("error when list files", e);
                }
            }
            try {
                out.writeLong(size);
                files.forEach(p -> {
                    File file = p.toFile();
                    boolean isDir = false;
                    if (file.isDirectory()) {
                        isDir = true;
                    }
                    try {
                        out.writeUTF(file.getName());
                        out.writeBoolean(isDir);
                    } catch (IOException e) {
                        throw new IllegalStateException("error while writing to client", e);
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException("error while writing to outputStream", e);
            }
        }

        private void get(String path) {
            long size = 0;
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                size = file.length();
            }
            try {
                out.writeLong(size);
            } catch (IOException e) {
                throw new IllegalStateException("error while writing size to outputStream", e);
            }

            if (file.exists() && file.isFile()) {
                try (BufferedInputStream bufIn =
                             new BufferedInputStream(new FileInputStream(path))) {

                    byte[] buffer = new byte[BUF_SIZE];
                    long readSize = 0;
                    do {
                        int temp = bufIn.read(buffer, 0, BUF_SIZE);
                        readSize += temp;
                        out.write(buffer, 0, temp);
                    } while (readSize < size);
                } catch (IOException e) {
                    throw new IllegalStateException("cannot read file", e);
                }
            }
        }
    }
}
