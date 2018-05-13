package ru.spbau.mit.tracker;

public final class TrackerConfig {
    public static final int SERVER_PORT = 8081;
    public static final int THREADS_COUNT = 8;
    public static final String TRACKER_RESOURCES = "./resources/";
    public static final String TRACKER_RESOURCES_ID_TO_FILE = TRACKER_RESOURCES + "idToFile";
    public static final String TRACKER_RESOURCES_ID_TO_CLIENT = TRACKER_RESOURCES + "idToClient";
    public static final String TRACKER_RESOURCES_CLIENT_LAST_UPD = TRACKER_RESOURCES + "ClientLastUpd";
    public static final String TRACKER_RESOURCES_LAST_ID = TRACKER_RESOURCES + "lastId";

    private TrackerConfig() {}
}
