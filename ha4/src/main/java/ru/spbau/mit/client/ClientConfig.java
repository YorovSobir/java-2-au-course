package ru.spbau.mit.client;

public final class ClientConfig {
    public static final String SERVER_HOST = "127.0.0.1";
    public static final int SERVER_PORT = 8081;
    public static final String CLIENT_RESOURCES = "./resources/";
    public static final String CLIENT_RESOURCES_ID_TO_PART = CLIENT_RESOURCES + "/idToPart";
    public static final String CLIENT_RESOURCES_ID_TO_PATH = CLIENT_RESOURCES + "/idToPath";
    public static final String CLIENT_RESOURCES_PATH_TO_ID = CLIENT_RESOURCES + "/pathToId";
    public static final int FILE_PART_SIZE = 1024;

    private ClientConfig() {}
}
