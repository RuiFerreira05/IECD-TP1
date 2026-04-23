package iecd.a51597.server.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

/**
 * Global server configuration loaded from {@code config.xml}.
 */
public final class ServerConfiguration {

    private static final String CONFIG_FILE = "config/config.xml";
    private static final Logger logger = LogManager.getLogger(ServerConfiguration.class);

    private ServerConfiguration() {}

    // CONSTS

    /** Default TCP listening port. */
    public static int DEFAULT_PORT = 5555;
    /** Maximum accepted frame payload size in bytes. */
    public static int MAX_FRAME_SIZE = 1024 * 1024;
    /** Session timeout in seconds. */
    public static long SESSION_TIMEOUT_SECONDS = 60 * 30; // 30 mins
    /** User persistence file path. */
    public static String USER_STORE = "data/users.xml";
    /** Width used by CLI status box rendering. */
    public static int STATUS_BOX_WIDTH = 42;
    /** Supported protocol version string. */
    public static String PROTOCOL_VERSION = "1.0";
    /** Which way to store data */
    public static String PERSISTENCE_TYPE = "xml";

    /**
     * Loads configuration overrides from {@code config.xml} when present and valid.
     */
    public static void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            logger.info("No config file found at '{}' — using defaults", CONFIG_FILE);
            return;
        }

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(file);

            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(ServerConfiguration.class.getResource("/schemas/config/config.xsd"));
            schema.newValidator().validate(new DOMSource(doc));

            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            DEFAULT_PORT = parseInt(root, "defaultPort", DEFAULT_PORT);
            MAX_FRAME_SIZE = parseInt(root, "maxFrameSize", MAX_FRAME_SIZE);
            SESSION_TIMEOUT_SECONDS = parseLong(root, "sessionTimeoutSeconds", SESSION_TIMEOUT_SECONDS);
            USER_STORE = parseString(root, "userStore", USER_STORE);
            STATUS_BOX_WIDTH = parseInt(root, "statusBoxWidth", STATUS_BOX_WIDTH);
            PROTOCOL_VERSION = parseString(root, "protocolVersion", PROTOCOL_VERSION);
            PERSISTENCE_TYPE = parseString(root, "persistenceType", PERSISTENCE_TYPE);

            logger.info("Configuration loaded from '{}'", CONFIG_FILE);
        } catch (Exception e) {
            logger.error("Failed to load config from '{}', using defaults", CONFIG_FILE, e);
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