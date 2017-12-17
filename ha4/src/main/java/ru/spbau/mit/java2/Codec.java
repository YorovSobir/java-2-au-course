package ru.spbau.mit.java2;

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
}
