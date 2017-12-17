package ru.spbau.mit.java2.client.api;

import ru.spbau.mit.java2.client.request.*;
import ru.spbau.mit.java2.client.response.*;
import ru.spbau.mit.java2.data.ClientInfo;
import ru.spbau.mit.java2.tracker.request.*;
import ru.spbau.mit.java2.tracker.response.*;

public interface TorrentClient extends AutoCloseable {
    ListResponse list();
    UploadResponse upload(String path);
    SourcesResponse sources(SourcesRequest request);
    UpdateResponse update(UpdateRequest request);

    GetClientResponse get(GetClientRequest request, ClientInfo clientInfo, String pathToStoreFile);
    StatClientResponse stat(StatClientRequest request, ClientInfo clientInfo);

    void start();
    void stop();
}
