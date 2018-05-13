package ru.spbau.mit;

import ru.spbau.mit.http.Request;
import ru.spbau.mit.http.Response;
import ru.spbau.mit.tracker.request.ListRequest;
import ru.spbau.mit.tracker.request.TrackerRequest;
import ru.spbau.mit.tracker.response.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Codec {

    private Codec() {}

    public static void serializeObject(String path, Object o) {
        try (FileOutputStream fos = new FileOutputStream(path);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos)) {
            objectOutputStream.writeObject(o);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object deserializeObject(String path) {
        File file = new File(path);
        Object obj = null;
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(path);
                 ObjectInputStream objectInputStream = new ObjectInputStream(fis)) {
                obj = objectInputStream.readObject();
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("unexpected exception", e);
            } catch (IOException | ClassNotFoundException e) {
                // cannot close auto-closable object
                // or cannot deserialize (ClassNotFound), continue without previous state
            }
        }
        return obj;
    }

    public static void mkdir(String path) {
        Path pathObj = Paths.get(path);
        if (!Files.exists(pathObj)) {
            try {
                Files.createDirectories(pathObj);
            } catch (IOException e) {
                throw new IllegalStateException("cannot create dir for server resources", e);
            }
        }
    }

    public static void writeObject(final ObjectOutputStream oos, Object obj) {
        try {
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new IllegalStateException("cannot write to socket output stream", e);
        }
    }

    public static Object readObject(ObjectInputStream ois) {
        try {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void writeRequest(final DataOutputStream dos, TrackerRequest obj) {
        try {
            Request request = new Request();
            request.setRequest(obj);
            request.setVersion(1,1);
            request.setAddress("localhost");
            dos.writeUTF(request.getString());
        } catch (IOException e) {
            throw new IllegalStateException("cannot write to socket output stream", e);
        }
    }

    public static TrackerRequest readRequest(DataInputStream dis) {
        try {
            return Request.parse(dis.readUTF());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void writeResponse(final DataOutputStream dos, TrackerResponse obj) {
        try {
            Response response = new Response();
            response.setCode(200);
            response.setVersion(1,1);
            response.setContentType("http/text");
            response.setData(obj);
            dos.writeUTF(response.getString());
        } catch (IOException e) {
            throw new IllegalStateException("cannot write to socket output stream", e);
        }
    }

    public static TrackerResponse readResponse(DataInputStream dis, int id) {
        try {
            switch (id) {
                case 1: return new ListResponse(Response.parse(dis.readUTF()).getData());
                case 2: return new UploadResponse(Response.parse(dis.readUTF()).getData());
                case 3: return new SourcesResponse(Response.parse(dis.readUTF()).getData());
                case 4: return new UpdateResponse(Response.parse(dis.readUTF()).getData());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
