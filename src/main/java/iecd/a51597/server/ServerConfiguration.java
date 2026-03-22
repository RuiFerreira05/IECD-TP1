package iecd.a51597.server;

public final class ServerConfiguration {

    private ServerConfiguration() {}

    public static final String USER_STORE = "data/users.xml";
    public static final String LEADERBOARD_STORE = "data/leaderboard.xml";
    public static final int MAX_FRAME_SIZE = 1024 * 1024;
    public static final int DEFAULT_PORT = 5555;
    public static final int SESSION_TIMEOUT_SECONDS = 60 * 30; // 30 mins
    public static final int STATUS_BOX_WIDTH = 46;
}
