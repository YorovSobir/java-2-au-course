package ru.spbau.mit.tracker.response;

import ru.spbau.mit.data.FileInfo;
import ru.spbau.mit.utils.BEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListResponse implements TrackerResponse {
    private int count;
    private List<FileInfo> fileInfos;
    private final BEncoder bEncoder = new BEncoder();

    public ListResponse(List<FileInfo> fileInfos) {
        count = fileInfos.size();
        this.fileInfos = fileInfos;
    }

    public ListResponse(String bEncode) {
        bEncoder.setInput(bEncode);
        fileInfos = new ArrayList<>();
        ArrayList<Object> objects = bEncoder.readAll();
        this.count = (int) objects.get(0);
        objects = (ArrayList<Object>) objects.get(1);
        for (Object map : objects) {
            int fileId = (int) ((Map<String, Object>) map).get("fileID");
            String name = (String) ((Map<String, Object>) map).get("name");
            int size = (int) ((Map<String, Object>) map).get("size");
            fileInfos.add(new FileInfo(fileId, name, size));
        }
    }

    public int getCount() {
        return count;
    }

    public List<FileInfo> getFileInfos() {
        return fileInfos;
    }

    @Override
    public String getString() {
        bEncoder.resetOutput();
        bEncoder.writeAll(count, fileInfos);
        return bEncoder.getString();
    }
}
