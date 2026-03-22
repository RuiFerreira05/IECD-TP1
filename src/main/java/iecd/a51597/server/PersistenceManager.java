package iecd.a51597.server.persistence;

import iecd.a51597.server.ServerConfiguration;
import iecd.a51597.server.store.User;
import iecd.a51597.server.store.UserStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.UUID;

public class PersistenceManager {

    private static final Logger logger = LogManager.getLogger(PersistenceManager.class);

    private final UserStore userStore;
    private final DocumentBuilderFactory dbf;
    private final TransformerFactory tf;

    public PersistenceManager(UserStore userStore) {
        this.userStore = userStore;
        this.dbf = DocumentBuilderFactory.newInstance();
        this.tf  = TransformerFactory.newInstance();
    }

    // ====== LOAD ======

    public void load() {
        loadUsers();
        // TODO: loadLeaderboard();
    }

    private void loadUsers() {
        File file = new File(ServerConfiguration.USER_STORE);
        if (!file.exists()) {
            logger.info("No users file found at '{}' — starting fresh", ServerConfiguration.USER_STORE);
            return;
        }

        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList userNodes = doc.getElementsByTagName("user");
            int count = 0;

            for (int i = 0; i < userNodes.getLength(); i++) {
                Element el = (Element) userNodes.item(i);
                try {
                    UUID   userId       = UUID.fromString(el.getAttribute("id"));
                    String username     = el.getAttribute("username");
                    String passwordHash = el.getAttribute("passwordHash");
                    String photo        = el.hasAttribute("photo") ? el.getAttribute("photo") : null;

                    userStore.loadUser(new User(userId, username, passwordHash, photo));
                    count++;
                } catch (Exception e) {
                    logger.warn("Skipping malformed user entry at index {}: {}", i, e.getMessage());
                }
            }

            logger.info("Loaded {} user(s) from '{}'", count, ServerConfiguration.USER_STORE);
        } catch (Exception e) {
            logger.error("Failed to load users from '{}': {}", ServerConfiguration.USER_STORE, e.getMessage());
        }
    }

    // ====== SAVE ======

    public void save() {
        saveUsers();
        // TODO: saveLeaderboard();
    }

    private void saveUsers() {
        try {
            File file = new File(ServerConfiguration.USER_STORE);
            file.getParentFile().mkdirs();

            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();
            doc.setXmlStandalone(true);

            Element root = doc.createElement("users");
            doc.appendChild(root);

            for (User user : userStore.getAllUsers()) {
                Element userEl = doc.createElement("user");
                userEl.setAttribute("id",           user.getUserId().toString());
                userEl.setAttribute("username",     user.getUsername());
                userEl.setAttribute("passwordHash", user.getPasswordHash());
                if (user.getPhoto() != null) {
                    userEl.setAttribute("photo", user.getPhoto());
                }
                root.appendChild(userEl);
            }

            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(file));

            logger.info("Saved {} user(s) to '{}'", userStore.getAllUsers().size(), ServerConfiguration.USER_STORE);
        } catch (Exception e) {
            logger.error("Failed to save users to '{}': {}", ServerConfiguration.USER_STORE, e.getMessage());
        }
    }
}