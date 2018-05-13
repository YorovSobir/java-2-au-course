package ru.spbau.mit.http;

import ru.spbau.mit.data.ClientDataInfo;
import ru.spbau.mit.tracker.request.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Request {
    private String args;
    private int major;
    private int minor;
    private String address;

    public Request() {

    }

    public void setRequest(TrackerRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request instanceof ListRequest) {
            sb.append("id=1");
        } else if (request instanceof UploadRequest) {
            UploadRequest uploadRequest = ((UploadRequest)request);
            sb.append("id=2");
            sb.append("&name=").append(uploadRequest.getName());
            sb.append("&size=").append(uploadRequest.getSize());
        } else if (request instanceof SourcesRequest) {
            SourcesRequest sourcesRequest = ((SourcesRequest)request);
            sb.append("id=3");
            sb.append("&fileID=").append(sourcesRequest.getFileId());
        } else if (request instanceof UpdateRequest) {
            UpdateRequest updateRequest = (UpdateRequest) request;
            sb.append("id=4");
            ClientDataInfo info = updateRequest.getClientDataInfo();
            sb.append("&port=").append(info.getClientPort());
            sb.append("&count=").append(info.getCount());
            for (int i = 0; i < info.getFilesId().size(); i++) {
                sb.append("&value").append(i).append("=").append(info.getFilesId().get(i).toString());
            }
        }
        args = sb.toString();
    }

    public static TrackerRequest parse(String message) {
        String[] values = message.split(" ")[1].split("\\?")[1].split("&");
        Map<String, String> keys = new HashMap<>();
        for (String v : values) {
            keys.put(v.split("=")[0], v.split("=")[1]);
        }
        switch (keys.get("id")) {
            case "1": return listRequest(keys);
            case "2": return uploadRequest(keys);
            case "3": return sourcesRequest(keys);
            case "4": return updateRequest(keys);
        }
        return null;
    }

    private static ListRequest listRequest(Map<String, String> keys) {
        return new ListRequest();
    }

    private static UploadRequest uploadRequest(Map<String, String> keys) {
        return new UploadRequest(keys.get("name"), Long.valueOf(keys.get("size")));
    }

    private static SourcesRequest sourcesRequest(Map<String, String> keys) {
        return new SourcesRequest(Integer.valueOf(keys.get("fileID")));
    }

    private static UpdateRequest updateRequest(Map<String, String> keys) {
        int port = Integer.valueOf(keys.get("port"));
        int size = Integer.valueOf(keys.get("count"));
        List<Integer> filesID = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            filesID.add(Integer.valueOf(keys.get("value" + String.valueOf(i))));
        }
        return new UpdateRequest(new ClientDataInfo(port, filesID));
    }

    public void setVersion(int major, int minor) {
        this.minor = minor;
        this.major = major;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getString() {
        return "GET " + address + "?" + args + " HTTP/" + String.valueOf(major) + "." + String.valueOf(minor);
    }
}
