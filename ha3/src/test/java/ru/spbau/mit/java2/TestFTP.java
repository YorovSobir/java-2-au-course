package ru.spbau.mit.java2;

import org.junit.AfterClass;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestFTP {
    private static final int SERVER_RUN_TIME = 100;
    private static final String HOST = "localhost";
    private static final int PORT = 4444;
    private static final FTPServerImpl server = new FTPServerImpl(PORT);
    private static Thread serverThread = new Thread(server::start);
    static {
        serverThread.start();
        try {
            //wait server running
            Thread.sleep(SERVER_RUN_TIME);
        } catch (InterruptedException ignored) {

        }
    }

    private static byte[] getContent(InputStream is) throws IOException {
        final int bufSize = 256;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufSize];
        int count;
        while ((count = is.read(buffer, 0, bufSize)) != - 1) {
            baos.write(buffer, 0, count);
        }
        return baos.toByteArray();
    }

    @Test
    public void simpleListTest() throws Exception {
        FTPClientImpl client = new FTPClientImpl(HOST, PORT);
        client.connect();

        DataInputStream dis = client.list(".");

        HashMap<String, Boolean> actualFiles = new LinkedHashMap<>();
        long actualSize = dis.readLong();
        for (int i = 0; i < actualSize; i++) {
            String path = dis.readUTF();
            boolean isDir = dis.readBoolean();
            actualFiles.put(path, isDir);
        }

        File[] expectedFiles = new File(".").listFiles();
        if (expectedFiles == null) {
            assertEquals(0, actualSize);
        } else {
            assertEquals(expectedFiles.length, actualSize);
            Arrays.stream(expectedFiles).forEach(f -> {
                assertTrue(actualFiles.containsKey(f.getName()));
                assertEquals(f.isDirectory(), actualFiles.get(f.getName()));
            });
        }

        client.disconnect();
    }

    @Test
    public void simpleGetTest() throws Exception {
        FTPClientImpl client = new FTPClientImpl(HOST, PORT);
        client.connect();

        DataInputStream dis = client.get("./build.gradle");
        long actualSize = dis.readLong();

        File file = new File("./build.gradle");
        if (!file.exists()) {
            assertEquals(0, actualSize);
        } else {
            assertEquals(file.length(), actualSize);

            assertArrayEquals(getContent(new FileInputStream(file.getPath())), getContent(dis));
        }

        client.disconnect();
    }

    @Test
    public void testNonExistentFile() throws Exception {
        FTPClientImpl client = new FTPClientImpl(HOST, PORT);
        client.connect();

        DataInputStream dis = client.get("./non-existent.file");
        assertEquals(0, dis.readLong());

        dis = client.list("./non-existent.dir");
        assertEquals(0, dis.readLong());
        client.disconnect();
    }

    @Test
    public void listTest() {
        List<FTPClientImpl> clients = new ArrayList<>();
        final int size = 7;
        for (int i = 0; i < size; ++i) {
            clients.add(new FTPClientImpl(HOST, PORT));
        }
        clients.forEach(FTPClientImpl::connect);
        List<byte[]> result = clients.parallelStream()
                .map(client -> {
                    try {
                        return getContent(client.list("."));
                    } catch (IOException e) {
                        return new byte[0];
                    }
                }).collect(Collectors.toList());

        result.forEach(a -> result.forEach(b -> assertArrayEquals(a, b)));
        clients.forEach(FTPClientImpl::disconnect);
    }

    @Test
    public void getTest() {
        List<FTPClientImpl> clients = new ArrayList<>();
        final int size = 7;
        for (int i = 0; i < size; ++i) {
            clients.add(new FTPClientImpl(HOST, PORT));
        }
        clients.forEach(FTPClientImpl::connect);

        List<byte[]> result = clients.parallelStream()
                .map(client -> {
                    try {
                        return getContent(client.get("./build.gradle"));
                    } catch (IOException e) {
                        return new byte[0];
                    }
                }).collect(Collectors.toList());

        result.forEach(a -> result.forEach(b -> assertArrayEquals(a, b)));
        clients.forEach(FTPClientImpl::disconnect);
    }

    @AfterClass
    public static void close() throws Exception {
        server.stop();
    }
}
