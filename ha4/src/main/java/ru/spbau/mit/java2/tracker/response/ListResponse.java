package ru.spbau.mit.java2.tracker.response;

import ru.spbau.mit.java2.data.FileInfo;

import java.io.Serializable;
import java.util.List;

public class ListResponse implements Serializable {
    private int count;
    private List<FileInfo> fileInfos;

    public ListResponse(List<FileInfo> fileInfos) {
        count = fileInfos.size();
        this.fileInfos = fileInfos;
    }

    public int getCount() {
        return count;
    }

    public List<FileInfo> getFileInfos() {
        return fileInfos;
    }

}
