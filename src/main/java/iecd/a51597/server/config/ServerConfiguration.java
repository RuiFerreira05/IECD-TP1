package iecd.a51597.server.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public final class ServerConfiguration {

    private static final String CONFIG_FILE = "config.xml";
    private static final Logger logger = LogManager.getLogger(ServerConfiguration.class);

    private ServerConfiguration() {}

    // CONSTS

    public static int DEFAULT_PORT = 5555;
    public static int MAX_FRAME_SIZE = 1024 * 1024;
    public static long SESSION_TIMEOUT_SECONDS = 60 * 30; // 30 mins
    public static String USER_STORE = "data/users.xml";
    public static String LEADERBOARD_STORE = "data/leaderboard.xml";
    public static int STATUS_BOX_WIDTH = 42;
    public static String PROTOCOL_VERSION = "1.0";

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            logger.info("No config file found at '{}' — using defaults", CONFIG_FILE);
            return;
        }

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            DEFAULT_PORT = parseInt(root, "defaultPort", DEFAULT_PORT);
            MAX_FRAME_SIZE = parseInt(root, "maxFrameSize", MAX_FRAME_SIZE);
            SESSION_TIMEOUT_SECONDS = parseLong(root, "sessionTimeoutSeconds", SESSION_TIMEOUT_SECONDS);
            USER_STORE = parseString(root, "userStore", USER_STORE);
            LEADERBOARD_STORE = parseString(root, "leaderboardStore", LEADERBOARD_STORE);
            PROTOCOL_VERSION = parseString(root, "protocolVersion", PROTOCOL_VERSION);

            logger.info("Configuration loaded from '{}'", CONFIG_FILE);
        } catch (Exception e) {
            logger.error("Failed to load config from '{}', using defaults: {}", CONFIG_FILE, e.getMessage());
        }
    }

    private static String parseString(Element root, String tag, String defaultValue) {
        var nodes = root.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return defaultValue;
        String value = nodes.item(0).getTextContent().trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private static int parseInt(Element root, String tag, int defaultValue) {
        try {
            return Integer.parseInt(parseString(root, tag, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid value for <{}>, using default: {}", tag, defaultValue);
            return defaultValue;
        }
    }

    private static long parseLong(Element root, String tag, long defaultValue) {
        try {
            return Long.parseLong(parseString(root, tag, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid value for <{}>, using default: {}", tag, defaultValue);
            return defaultValue;
        }
    }
}