package ru.spbau.mit.client.api;

import ru.spbau.mit.client.request.GetClientRequest;
import ru.spbau.mit.client.request.StatClientRequest;
import ru.spbau.mit.client.response.GetClientResponse;
import ru.spbau.mit.client.response.StatClientResponse;
import ru.spbau.mit.data.ClientInfo;
import ru.spbau.mit.tracker.request.SourcesRequest;
import ru.spbau.mit.tracker.request.UpdateRequest;
import ru.spbau.mit.tracker.response.ListResponse;
import ru.spbau.mit.tracker.response.SourcesResponse;
import ru.spbau.mit.tracker.response.UpdateResponse;
import ru.spbau.mit.tracker.response.UploadResponse;

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
