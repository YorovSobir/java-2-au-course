package ru.spbau.mit.tracker;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import ru.spbau.mit.CommonConfig;
import ru.spbau.mit.client.ClientConfig;
import ru.spbau.mit.client.TorrentClientImpl;
import ru.spbau.mit.client.api.TorrentClient;
import ru.spbau.mit.client.request.GetClientRequest;
import ru.spbau.mit.client.request.StatClientRequest;
import ru.spbau.mit.client.response.StatClientResponse;
import ru.spbau.mit.data.ClientDataInfo;
import ru.spbau.mit.data.ClientInfo;
import ru.spbau.mit.data.FileInfo;
import ru.spbau.mit.tracker.api.TorrentTracker;
import ru.spbau.mit.tracker.request.SourcesRequest;
import ru.spbau.mit.tracker.request.UpdateRequest;
import ru.spbau.mit.tracker.response.ListResponse;
import ru.spbau.mit.tracker.response.SourcesResponse;
import ru.spbau.mit.tracker.response.UpdateResponse;
import ru.spbau.mit.tracker.response.UploadResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TorrentTrackerTest {
    private static TorrentTracker tracker;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws InterruptedException {
        tracker = new TorrentTrackerImpl();
        new Thread(() -> tracker.start()).start();
        Thread.sleep(10);
    }

    private static TorrentClient clientSetUp(int port) {
        TorrentClient client = new TorrentClientImpl(port);
        client.start();
        return client;
    }

    @Test
    public void testTrackerUpload() throws Exception {
        final int clientPort = 11111;
        try (TorrentClient client = clientSetUp(clientPort)) {
            final String path = "./build.gradle";
            UploadResponse response = client.upload(path);
            ListResponse listResponse = client.list();
            Assert.assertTrue(listResponse.getFileInfos().contains(getFileInfo(response.getFileId(), path)));
        }
    }

    @Test
    public void testTrackerUpdate() throws Exception {
        final int clientPort = 11111;
        try (TorrentClient client = clientSetUp(clientPort)) {
            List<Integer> filesId = Files.walk(Paths.get("."))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .map(client::upload)
                    .map(UploadResponse::getFileId)
                    .collect(Collectors.toList());
            Assert.assertTrue(client.update(new UpdateRequest(
                    new ClientDataInfo(clientPort, filesId))).isStatus());
        }
    }

    @Test
    public void testTrackerList() throws Exception {
        final int port = 11111;
        try (TorrentClient client = clientSetUp(port)) {
            List<FileInfo> expectedFileInfos = Files.walk(Paths.get("./src/"))
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        UploadResponse response = client.upload(path.toString());
                        try {
                            return new FileInfo(response.getFileId(),
                                    path.getFileName().toString(), Files.size(path));
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .collect(Collectors.toList());
            ListResponse response = client.list();
            Assert.assertTrue(expectedFileInfos.stream()
                    .allMatch(fileInfo -> response.getFileInfos().contains(fileInfo)));
        }
    }

    @Test
    public void testTrackerSources() throws Exception {
        File tempFile = folder.newFile();
        final int port = 11111;
        try (TorrentClient torrentClient = clientSetUp(port)) {
            UploadResponse response = torrentClient.upload(tempFile.getPath());
            List<Integer> ports = Arrays.asList(22222, 33333, 44444);
            ports.forEach(p -> {
                try (TorrentClient client = clientSetUp(p)) {
                    client.update(new UpdateRequest(
                            new ClientDataInfo(p, Collections.singletonList(response.getFileId()))));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            SourcesResponse sourcesResponse =
                    torrentClient.sources(new SourcesRequest(response.getFileId()));
            Assert.assertTrue(ports.stream()
                    .allMatch(p -> sourcesResponse.getClients()
                            .contains(new ClientInfo(localAddr(), p))));
        }
    }

    @Test
    public void testUploadSameFileFromOneClient() throws Exception {
        File tempFile = folder.newFile();
        final int port = 11111;
        try (TorrentClient torrentClient = clientSetUp(port)) {
            UploadResponse response = torrentClient.upload(tempFile.getPath());
            UploadResponse responseSame = torrentClient.upload(tempFile.getPath());
            Assert.assertEquals(response.getFileId(), responseSame.getFileId());
        }
    }


    @Test
    public void testTimeOut() throws Exception {
        File tempFile = folder.newFile();
        final int port = 11111;
        UploadResponse uploadResponse;
        try (TorrentClient torrentClient = clientSetUp(port)) {
            uploadResponse = torrentClient.upload(tempFile.getPath());
            UpdateResponse updateResponse = torrentClient.update(new UpdateRequest(
                    new ClientDataInfo(port, Collections.singletonList(uploadResponse.getFileId()))));
        }
        Thread.sleep(2 * CommonConfig.TIMEOUT + 1);
        try (TorrentClient torrentClient = clientSetUp(port)) {
            SourcesResponse sourcesResponse =
                    torrentClient.sources(new SourcesRequest(uploadResponse.getFileId()));
            Assert.assertTrue(sourcesResponse.getClients().isEmpty());
        }
    }

    @Test
    public void testStat() throws Exception {
        File tempFile = folder.newFile();
        final int port = 11111;
        try (TorrentClient torrentClient = clientSetUp(port)) {
            UploadResponse uploadResponse = uploadFile(torrentClient, port, tempFile, 2096);

            final int port1 = 22222;
            try (TorrentClient torrentClient1 = clientSetUp(port1)) {
                SourcesResponse sourcesResponse =
                        torrentClient1.sources(new SourcesRequest(uploadResponse.getFileId()));
                Assert.assertTrue(sourcesResponse.getSize() > 0);
                StatClientResponse statClientResponse = torrentClient1.stat(
                        new StatClientRequest(uploadResponse.getFileId()),
                        sourcesResponse.getClients().get(0));

                long expectedPartCount = tempFile.length() / ClientConfig.FILE_PART_SIZE
                        + (tempFile.length() % ClientConfig.FILE_PART_SIZE == 0 ? 0 : 1);
                Assert.assertEquals(expectedPartCount, statClientResponse.getCount());
            }
        }
    }

    private static UploadResponse uploadFile(TorrentClient torrentClient, int port, File tempFile, int fileSize)
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            Random random = new Random();
            byte[] data = new byte[fileSize];
            random.nextBytes(data);
            fos.write(data);
        }
        UploadResponse uploadResponse = torrentClient.upload(tempFile.getPath());
        torrentClient.update(new UpdateRequest(
                new ClientDataInfo(port, Collections.singletonList(uploadResponse.getFileId()))
        ));
        return uploadResponse;
    }

    @Test
    public void testGet() throws Exception {
        File tempFile = folder.newFile();
        final int port = 11111;
        try (TorrentClient torrentClient = clientSetUp(port)) {
            UploadResponse uploadResponse = uploadFile(torrentClient, port, tempFile, 4096);

            final int port1 = 22222;
            try (TorrentClient torrentClient1 = clientSetUp(port1)) {
                SourcesResponse sourcesResponse =
                        torrentClient1.sources(new SourcesRequest(uploadResponse.getFileId()));
                Assert.assertTrue(sourcesResponse.getSize() > 0);
                StatClientResponse statClientResponse = torrentClient1.stat(
                        new StatClientRequest(uploadResponse.getFileId()), sourcesResponse.getClients().get(0));

                for (int part: statClientResponse.getFileParts()) {
                    torrentClient1.get(new GetClientRequest(uploadResponse.getFileId(), part),
                            sourcesResponse.getClients().get(0), getNewFilePath(uploadResponse.getFileId()));
                }

                Assert.assertArrayEquals(readAllBytes(tempFile.getPath()),
                        readAllBytes(getNewFilePath(uploadResponse.getFileId())));
            }
        }
    }

    @Test
    public void testGetFromSeveralSources() throws Exception {
        File file = folder.newFile();
        final int port1 = 11111;
        try (TorrentClient torrentClient1 = clientSetUp(port1)) {

            UploadResponse uploadResponse = uploadFile(torrentClient1, port1, file, 2000);
            ClientInfo clientInfo1 = new ClientInfo(localAddr(), port1);

            final int port2 = 22222;
            try (TorrentClient torrentClient2 = clientSetUp(port2)) {
                ClientInfo clientInfo2 = new ClientInfo(localAddr(), port2);
                StatClientResponse statClientResponse =
                        torrentClient2.stat(new StatClientRequest(uploadResponse.getFileId()), clientInfo1);

                Assert.assertTrue(statClientResponse.getCount() == 2);

                for (int part: statClientResponse.getFileParts()) {
                    torrentClient2.get(new GetClientRequest(uploadResponse.getFileId(), part),
                            clientInfo1, getNewFilePath(uploadResponse.getFileId()));
                }

                Assert.assertArrayEquals(readAllBytes(file.getPath()),
                        readAllBytes(getNewFilePath(uploadResponse.getFileId())));

                torrentClient2.update(new UpdateRequest(
                        new ClientDataInfo(port2, statClientResponse.getFileParts())));

                File fileRes = folder.newFile();
                final int port3 = 33333;
                try (TorrentClient torrentClient3 = clientSetUp(port3)) {
                    torrentClient3.get(new GetClientRequest(uploadResponse.getFileId(), 0),
                            clientInfo1, fileRes.getPath());

                    torrentClient3.get(new GetClientRequest(uploadResponse.getFileId(), 1),
                            clientInfo2, fileRes.getPath());

                    Assert.assertArrayEquals(readAllBytes(file.getPath()), readAllBytes(fileRes.getPath()));

                    File fileRes2 = folder.newFile();
                    final int port4 = 44444;
                    TorrentClient torrentClient4 = clientSetUp(port4);
                    Assert.assertTrue(statClientResponse.getCount() == 2);
                    torrentClient4.get(new GetClientRequest(uploadResponse.getFileId(), 1),
                            clientInfo1, fileRes2.getPath());

                    torrentClient4.get(new GetClientRequest(uploadResponse.getFileId(), 0),
                            clientInfo2, fileRes2.getPath());

                    Assert.assertArrayEquals(readAllBytes(file.getPath()), readAllBytes(fileRes2.getPath()));
                }
            }
        }
    }


    @Test
    public void testTrackerSerialized() throws Exception {
        File tempFile = folder.newFile();
        final int port = 11111;
        UploadResponse uploadResponse;
        try (TorrentClient torrentClient = clientSetUp(port)) {
            uploadResponse = uploadFile(torrentClient, port, tempFile, 4096);
        }
        tracker.stop();
        setUp();

        try (TorrentClient torrentClient = clientSetUp(port)) {
            SourcesResponse sourcesResponse = torrentClient.sources(new SourcesRequest(uploadResponse.getFileId()));
            Assert.assertTrue(sourcesResponse.getClients().size() == 1);
            Assert.assertTrue(sourcesResponse.getClients().contains(new ClientInfo(localAddr(), port)));
        }
    }

    @Before
    public void clean() throws IOException {
        File serverData = new File(TrackerConfig.TRACKER_RESOURCES);
        File clientData = new File(ClientConfig.CLIENT_RESOURCES);
        File data = new File("./data");
        removeTempDirs(serverData, clientData, data);
        data.mkdir();
        clientData.mkdir();
        serverData.mkdir();
    }

    private static void removeTempDirs(File serverData, File clientData, File data) throws IOException {
        delete(serverData);
        delete(clientData);
        delete(data);
    }

    private static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            if (file.list().length == 0) {
                file.delete();
            } else {
                String[] files = file.list();
                for (String temp : files) {
                    File fileDelete = new File(file, temp);
                    delete(fileDelete);
                }

                if (file.list().length == 0) {
                    file.delete();
                }
            }

        } else {
            file.delete();
        }
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        tracker.stop();
        File serverData = new File(TrackerConfig.TRACKER_RESOURCES);
        File clientData = new File(ClientConfig.CLIENT_RESOURCES);
        File data = new File("./data");
        removeTempDirs(serverData, clientData, data);
    }

    private static String getNewFilePath(int fileId) {
        return "./data/" + String.valueOf(fileId);
    }

    private byte[] readAllBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    private byte[] localAddr() {
        return new byte[]{127, 0, 0, 1};
    }

    private static FileInfo getFileInfo(int id, String path) {
        long size;
        String name;
        try {
            Path pathObj = Paths.get(path);
            size = Files.size(pathObj);
            name = pathObj.getFileName().toString();
        } catch (IOException e) {
            throw new IllegalStateException("file not found", e);
        }
        return new FileInfo(id, name, size);
    }
}
