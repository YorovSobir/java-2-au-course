package ru.spbau.mit.data;

import java.io.Serializable;

public class FileInfo implements Serializable {
    private int fileId;
    private String name;
    private long size;

    public FileInfo(int id, String name, long size) {
        this.fileId = id;
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public int getFileId() {
        return fileId;
    }

    @Override
    public int hashCode() {
        return fileId;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FileInfo) {
            FileInfo otherFileInfo = (FileInfo) other;
            return fileId == otherFileInfo.getFileId()
                    && name.equals(otherFileInfo.getName())
                    && size == otherFileInfo.getSize();
        }
        return other.equals(this);
    }
}
