package ru.spbau.mit.client;

import ru.spbau.mit.Codec;
import ru.spbau.mit.CommonConfig;
import ru.spbau.mit.client.request.GetClientRequest;
import ru.spbau.mit.client.request.StatClientRequest;
import ru.spbau.mit.client.response.GetClientResponse;
import ru.spbau.mit.client.response.StatClientResponse;
import ru.spbau.mit.common.api.Request;
import ru.spbau.mit.common.api.RequestConfig;
import ru.spbau.mit.data.ClientDataInfo;
import ru.spbau.mit.data.ClientInfo;
import ru.spbau.mit.data.FileContent;
import ru.spbau.mit.tracker.TrackerConfig;
import ru.spbau.mit.client.api.TorrentClient;
import ru.spbau.mit.tracker.request.ListRequest;
import ru.spbau.mit.tracker.request.SourcesRequest;
import ru.spbau.mit.tracker.request.UpdateRequest;
import ru.spbau.mit.tracker.request.UploadRequest;
import ru.spbau.mit.tracker.response.ListResponse;
import ru.spbau.mit.tracker.response.SourcesResponse;
import ru.spbau.mit.tracker.response.UpdateResponse;
import ru.spbau.mit.tracker.response.UploadResponse;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TorrentClientImpl implements TorrentClient {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService = Executors.newFixedThreadPool(TrackerConfig.THREADS_COUNT);
    private Map<Integer, List<Integer>> idToParts = new ConcurrentHashMap<>();
    private Map<Integer, String> idToPath = new ConcurrentHashMap<>();
    private Map<String, Integer> pathToId = new ConcurrentHashMap<>();
//    private ObjectOutputStream objectOutputStream;
//    private ObjectInputStream objectInputStream;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private Socket socket;

    public TorrentClientImpl(int port) {
        this.port = port;
        deserialize();
    }

    @Override
    public synchronized ListResponse list() {
        Codec.writeRequest(dataOutputStream, new ListRequest());
        return (ListResponse) Codec.readResponse(dataInputStream, 1);
//        Codec.writeObject(objectOutputStream, new ListRequest());
//        return (ListResponse) Codec.readObject(objectInputStream);
    }

    @Override
    public UploadResponse upload(String path) {
        Path p = Paths.get(path);
        if (pathToId.containsKey(p.toAbsolutePath().toString())) {
            return new UploadResponse(pathToId.get(p.toAbsolutePath().toString()));
        }
        long size;
        try {
            size = Files.size(p);
        } catch (IOException e) {
            throw new IllegalStateException("size of file undefined", e);
        }
        UploadRequest request = new UploadRequest(p.getFileName().toString(), size);
        UploadResponse response;
        synchronized (this) {
            Codec.writeRequest(dataOutputStream, request);
            response = (UploadResponse) Codec.readResponse(dataInputStream, 2);
//            Codec.writeObject(objectOutputStream, request);
//            response = (UploadResponse) Codec.readObject(objectInputStream);
        }
        storePath(response.getFileId(), p.toAbsolutePath().toString());
        List<Integer> fileParts = new ArrayList<>();
        long count = size / ClientConfig.FILE_PART_SIZE + (size % ClientConfig.FILE_PART_SIZE == 0 ? 0 : 1);
        for (int i = 0; i < count; i++) {
            fileParts.add(i);
        }
        storeParts(response.getFileId(), fileParts);
        return response;
    }

    @Override
    public synchronized SourcesResponse sources(SourcesRequest request) {
        Codec.writeRequest(dataOutputStream, request);
        return (SourcesResponse) Codec.readResponse(dataInputStream, 3);
//        Codec.writeObject(objectOutputStream, request);
//        return (SourcesResponse) Codec.readObject(objectInputStream);
    }

    @Override
    public synchronized UpdateResponse update(UpdateRequest request) {
        Codec.writeRequest(dataOutputStream, request);
        return (UpdateResponse) Codec.readResponse(dataInputStream, 4);
//        Codec.writeObject(objectOutputStream, request);
//        return (UpdateResponse) Codec.readObject(objectInputStream);
    }

    @Override
    public GetClientResponse get(GetClientRequest request, ClientInfo clientInfo, String pathToStoreFile) {
        try (Socket tempSocket =
                     new Socket(InetAddress.getByAddress(clientInfo.getClientIp()), clientInfo.getClientPort());
             ObjectInputStream ois = new ObjectInputStream(tempSocket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(tempSocket.getOutputStream())) {
            Codec.writeObject(oos, request);
            GetClientResponse response = (GetClientResponse) Codec.readObject(ois);
            long partSize = response.getFileContent().getContent().length;
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(pathToStoreFile, "rwd")) {
                if (randomAccessFile.length() < request.getFilePart() * ClientConfig.FILE_PART_SIZE + partSize) {
                    if (partSize < ClientConfig.FILE_PART_SIZE) {
                        randomAccessFile.setLength(request.getFilePart() * ClientConfig.FILE_PART_SIZE + partSize);
                    } else {
                        randomAccessFile.setLength((request.getFilePart() + 1) * ClientConfig.FILE_PART_SIZE);
                    }
                }
                randomAccessFile.seek(request.getFilePart() * ClientConfig.FILE_PART_SIZE);
                randomAccessFile.write(response.getFileContent().getContent());
            }
            storePath(request.getFileId(), pathToStoreFile);
            List<Integer> fileParts = new ArrayList<>();
            fileParts.add(request.getFilePart());
            storeParts(request.getFileId(), fileParts);
            return response;
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException("cannot create socket", e);
        }
    }

    @Override
    public void start() {
        Runnable serverRun = () -> {
            try (ServerSocket server = new ServerSocket(port)) {
                serverSocket = server;
                while (true) {
                    Socket client = serverSocket.accept();
                    executorService.submit(new ClientHandler(client));
                }
            } catch (SocketException e) {
                // socket closed
            } catch (IOException e) {
                throw new IllegalStateException("Could not listen on port: " + port, e);
            }
        };
        executorService.submit(serverRun);
        try {
            socket = new Socket(ClientConfig.SERVER_HOST, ClientConfig.SERVER_PORT);
        } catch (IOException e) {
            throw new IllegalStateException("Could not connect to server: "
                    + ClientConfig.SERVER_HOST + " in port " + ClientConfig.SERVER_PORT, e);
        }
        try {
//            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
//            objectInputStream = new ObjectInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        executorService.submit(new Updater());
    }

    @Override
    public void stop() {
        if (serverSocket == null || socket == null) {
            return;
        }
        try {
            serverSocket.close();
            socket.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        executorService.shutdown();
        serialize();
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    private class Updater implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(CommonConfig.TIMEOUT);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
                ClientDataInfo clientDataInf = new ClientDataInfo(port, new ArrayList<>(idToParts.keySet()));
                UpdateRequest request = new UpdateRequest(clientDataInf);
                UpdateResponse response;
                do {
                    response = update(request);
                } while (!response.isStatus());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void deserialize() {
        idToParts = (Map<Integer, List<Integer>>)
                Codec.deserializeObject(ClientConfig.CLIENT_RESOURCES_ID_TO_PART);
        idToParts = idToParts == null ? new ConcurrentHashMap<>() : idToParts;

        idToPath = (Map<Integer, String>) Codec.deserializeObject(ClientConfig.CLIENT_RESOURCES_ID_TO_PATH);
        idToPath = idToPath == null ? new ConcurrentHashMap<>() : idToPath;

        pathToId = (Map<String, Integer>) Codec.deserializeObject(ClientConfig.CLIENT_RESOURCES_PATH_TO_ID);
        pathToId = pathToId == null ? new ConcurrentHashMap<>() : pathToId;
    }

    private void serialize() {
        Codec.serializeObject(ClientConfig.CLIENT_RESOURCES_ID_TO_PART, idToParts);
        Codec.serializeObject(ClientConfig.CLIENT_RESOURCES_ID_TO_PATH, idToPath);
        Codec.serializeObject(ClientConfig.CLIENT_RESOURCES_PATH_TO_ID, pathToId);
    }

    private final class ClientHandler implements Runnable {
        private Socket client;

        private ClientHandler(Socket client) {
            this.client = client;
        }


        @Override
        public void run() {
                try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {

                while (true) {
                    Request request = (Request) in.readObject();
                    switch (request.getType()) {
                        case RequestConfig.STAT_REQUEST:
                            stat((StatClientRequest) request, out);
                            break;
                        case RequestConfig.GET_REQUEST:
                            get((GetClientRequest) request, out);
                            break;
                        default:
                            throw new UnsupportedOperationException("undefined type of request");
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("cannot open client socket's stream", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        private void stat(StatClientRequest request, ObjectOutputStream out) {
            StatClientResponse response = new StatClientResponse(idToParts.get(request.getFileId()));
            Codec.writeObject(out, response);
        }

        private void get(GetClientRequest request, ObjectOutputStream out) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(getPath(request), "r")) {
                randomAccessFile.seek(request.getFilePart() * ClientConfig.FILE_PART_SIZE);
                byte[] data;
                if (randomAccessFile.length() < (request.getFilePart() + 1) * ClientConfig.FILE_PART_SIZE) {
                    data = new byte[(int) randomAccessFile.length()
                            - request.getFilePart() * ClientConfig.FILE_PART_SIZE];
                } else {
                    data = new byte[ClientConfig.FILE_PART_SIZE];
                }
                randomAccessFile.read(data);
                Codec.writeObject(out, new GetClientResponse(new FileContent(data)));
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("requested file not found", e);
            } catch (IOException e) {
                throw new IllegalStateException("illegal part number", e);
            }
        }

        private String getPath(GetClientRequest request) {
            return idToPath.get(request.getFileId());
        }
    }

    private void storePath(int fileId, String path) {
        idToPath.put(fileId, path);
        pathToId.put(path, fileId);
    }

    private void storeParts(int fileId, List<Integer> fileParts) {
        if (idToParts.containsKey(fileId)) {
            idToParts.get(fileId).addAll(fileParts);
        } else {
            idToParts.put(fileId, fileParts);
        }
    }

    @Override
    public StatClientResponse stat(StatClientRequest request, ClientInfo clientInfo) {
        try (Socket tempSocket =
                     new Socket(InetAddress.getByAddress(clientInfo.getClientIp()), clientInfo.getClientPort());
             ObjectOutputStream oos = new ObjectOutputStream(tempSocket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(tempSocket.getInputStream())) {
            Codec.writeObject(oos, request);
            return (StatClientResponse) Codec.readObject(ois);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException("cannot create socket", e);
        }
    }

}
