package ru.spbau.mit.java2.client.response;

import ru.spbau.mit.java2.data.FileContent;

import java.io.Serializable;

public class GetClientResponse implements Serializable {
    private FileContent fileContent;

    public GetClientResponse(FileContent fileContent) {
        this.fileContent = fileContent;
    }

    public FileContent getFileContent() {
        return fileContent;
    }
}
