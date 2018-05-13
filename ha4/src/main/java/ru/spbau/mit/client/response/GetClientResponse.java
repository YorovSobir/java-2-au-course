package ru.spbau.mit.client.response;

import ru.spbau.mit.data.FileContent;

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
