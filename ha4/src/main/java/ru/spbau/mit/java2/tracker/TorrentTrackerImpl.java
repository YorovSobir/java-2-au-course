package ru.spbau.mit.java2.tracker;

import ru.spbau.mit.java2.Codec;
import ru.spbau.mit.java2.CommonConfig;
import ru.spbau.mit.java2.common.api.Request;
import ru.spbau.mit.java2.common.api.RequestConfig;
import ru.spbau.mit.java2.data.ClientInfo;
import ru.spbau.mit.java2.data.FileInfo;
import ru.spbau.mit.java2.tracker.api.TorrentTracker;
import ru.spbau.mit.java2.tracker.request.*;
import ru.spbau.mit.java2.tracker.response.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ru.spbau.mit.java2.Codec.readObject;
import static ru.spbau.mit.java2.Codec.writeObject;

public class TorrentTrackerImpl implements TorrentTracker {

    private ExecutorService executorService = Executors.newFixedThreadPool(TrackerConfig.THREADS_COUNT);
    private ServerSocket serverSocket;
    private ConcurrentHashMap<Integer, FileInfo> fileIdToFileInfo = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, HashSet<ClientInfo>> fileIdToClientInfo = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ClientInfo, Long> clientInfoLastUpd = new ConcurrentHashMap<>();
    private AtomicInteger lastFileId = new AtomicInteger(0);

    public TorrentTrackerImpl() {
        Codec.mkdir(TrackerConfig.TRACKER_RESOURCES);
        deserialize();
    }

    @Override
    public void start() {
        try (ServerSocket server = new ServerSocket(TrackerConfig.SERVER_PORT)) {
            serverSocket = server;
            executorService.submit(new Updater());
            while (true) {
                Socket client = serverSocket.accept();
                executorService.submit(new ClientHandler(client));
            }
        } catch (SocketException e) {
            // socket closed
        } catch (IOException e) {
            throw new IllegalStateException("Could not listen on port: " + TrackerConfig.SERVER_PORT, e);
        }
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
                    break;
                }
                long currentTime = System.currentTimeMillis();
                for (Integer id: fileIdToClientInfo.keySet()) {
                    HashSet<ClientInfo> clients = fileIdToClientInfo.get(id);
                    for (ClientInfo clientInfo: clients) {
                        if (currentTime - clientInfoLastUpd.get(clientInfo) > CommonConfig.TIMEOUT) {
                            clients.remove(clientInfo);
                        }
                    }
                }
            }
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
        executorService.shutdown();
        serialize();
    }

    @SuppressWarnings("unchecked")
    private void deserialize() {
        fileIdToFileInfo = (ConcurrentHashMap<Integer, FileInfo>)
                Codec.deserializeObject(TrackerConfig.TRACKER_RESOURCES_ID_TO_FILE);
        fileIdToFileInfo = fileIdToFileInfo == null ? new ConcurrentHashMap<>() : fileIdToFileInfo;

        fileIdToClientInfo = (ConcurrentHashMap<Integer, HashSet<ClientInfo>>)
                Codec.deserializeObject(TrackerConfig.TRACKER_RESOURCES_ID_TO_CLIENT);
        fileIdToClientInfo = fileIdToClientInfo == null ? new ConcurrentHashMap<>() : fileIdToClientInfo;

        clientInfoLastUpd = (ConcurrentHashMap<ClientInfo, Long>)
                Codec.deserializeObject(TrackerConfig.TRACKER_RESOURCES_CLIENT_LAST_UPD);
        clientInfoLastUpd = clientInfoLastUpd == null ? new ConcurrentHashMap<>() : clientInfoLastUpd;

        lastFileId = (AtomicInteger) Codec.deserializeObject(TrackerConfig.TRACKER_RESOURCES_LAST_ID);
        lastFileId = lastFileId == null ? new AtomicInteger(0) : lastFileId;
    }

    private void serialize() {
        Codec.serializeObject(TrackerConfig.TRACKER_RESOURCES_ID_TO_FILE, fileIdToFileInfo);
        Codec.serializeObject(TrackerConfig.TRACKER_RESOURCES_ID_TO_CLIENT, fileIdToClientInfo);
        Codec.serializeObject(TrackerConfig.TRACKER_RESOURCES_CLIENT_LAST_UPD, clientInfoLastUpd);
        Codec.serializeObject(TrackerConfig.TRACKER_RESOURCES_LAST_ID, lastFileId);
    }

    private static boolean isOnline(long lastUpd, long curTime) {
        return (curTime - lastUpd) < CommonConfig.TIMEOUT;
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
                    Request request = (Request) readObject(in);
                    switch (request.getType()) {
                        case RequestConfig.LIST_REQUEST:
                            list(out);
                            break;
                        case RequestConfig.UPLOAD_REQUEST:
                            upload((UploadRequest) request, out);
                            break;
                        case RequestConfig.SOURCES_REQUEST:
                            sources((SourcesRequest) request, out);
                            break;
                        case RequestConfig.UPDATE_REQUEST:
                            update((UpdateRequest) request, out);
                            break;
                        default:
                            throw new UnsupportedOperationException("undefined type of request");
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("cannot open client socket's stream", e);
            }
        }

        private void list(ObjectOutputStream out) throws IOException {
            writeObject(out, new ListResponse(new ArrayList<>(fileIdToFileInfo.values())));
        }

        private void upload(UploadRequest request, ObjectOutputStream out) throws IOException {
            Integer fileId = lastFileId.getAndIncrement();
            fileIdToFileInfo.put(fileId, new FileInfo(fileId, request.getName(), request.getSize()));
            fileIdToClientInfo.put(fileId, new HashSet<>());
            writeObject(out, new UploadResponse(fileId));
        }

        private void sources(SourcesRequest request, ObjectOutputStream out) throws IOException {
            long curTime = System.currentTimeMillis();
            List<ClientInfo> clientInfos = fileIdToClientInfo.get(request.getFileId())
                    .stream()
                    .filter(clientInfo -> isOnline(clientInfoLastUpd.get(clientInfo), curTime))
                    .collect(Collectors.toList());
            writeObject(out, new SourcesResponse(clientInfos));
        }

        private void update(UpdateRequest request, ObjectOutputStream out) throws IOException {
            ClientInfo clientInfo = new ClientInfo(client.getInetAddress().getAddress(),
                    request.getClientDataInfo().getClientPort());
            clientInfoLastUpd.put(clientInfo, System.currentTimeMillis());
            boolean result = request.getClientDataInfo().getFilesId().stream().allMatch(id -> {
                    if (fileIdToFileInfo.containsKey(id)) {
                        fileIdToClientInfo.get(id).add(clientInfo);
                        return true;
                    }
                    return false;
                });
            writeObject(out, new UpdateResponse(result));
        }
    }
}
