package iecd.a51597.server.persistence;

import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.store.PlayerStats;
import iecd.a51597.server.store.User;
import iecd.a51597.server.store.UserStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles XML persistence of server state (currently user repository).
 */
public class PersistenceManager {

    private static final Logger logger = LogManager.getLogger(PersistenceManager.class);
    private final UserRepository userRepository;

    private final UserStore userStore;

    /**
     * Creates a persistence manager for a user store.
     *
     * @param userStore target user store
     */
    public PersistenceManager(UserStore userStore) {
        this.userStore = userStore;
        this.userRepository = RepositoryFactory.createUserRepository(ServerConfiguration.PERSISTENCE_TYPE, logger);
    }

    /**
     * Loads all persisted data into memory.
     */
    public void load() {
        userRepository.loadInto(userStore);
    }

    /**
     * Saves in-memory state to persistence files.
     */
    public void save() {
        userRepository.saveFrom(userStore);
    }
}