package iecd.a51597.server.persistence.impl;

import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.persistence.UserRepository;
import iecd.a51597.common.store.PlayerStats;
import iecd.a51597.common.store.User;
import iecd.a51597.server.store.UserStore;
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

public class XmlUserRepository implements UserRepository {

    private final DocumentBuilderFactory dbf;
    private final TransformerFactory tf;
    private final Schema userSchema;

    private final Logger logger;

    public XmlUserRepository(Logger logger) {
        this.dbf = DocumentBuilderFactory.newInstance();
        this.tf = TransformerFactory.newInstance();
        this.logger = logger;

        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            this.userSchema = sf.newSchema(getClass().getResource("/users.xsd"));
        } catch (SAXException e) {
            throw new IllegalStateException("Failed to load users.xsd — ensure it is on the classpath", e);
        }
    }

    private Validator newValidator() {
        return userSchema.newValidator();
    }

    private static double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public void loadInto(UserStore userStore) {
        File file = new File(ServerConfiguration.USER_STORE);
        if (!file.exists()) {
            logger.info("No users file found at '{}' — starting fresh", ServerConfiguration.USER_STORE);
            return;
        }

        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            try {
                newValidator().validate(new DOMSource(doc));
            } catch (SAXException e) {
                logger.error("Users file '{}' failed schema validation — aborting load: {}",
                        ServerConfiguration.USER_STORE, e.getMessage());
                return;
            }

            NodeList userNodes = doc.getElementsByTagName("user");
            int count = 0;

            for (int i = 0; i < userNodes.getLength(); i++) {
                Element el = (Element) userNodes.item(i);
                try {
                    UUID userId = UUID.fromString(el.getAttribute("id"));
                    String username = el.getAttribute("username");
                    String passwordHash = el.getAttribute("passwordHash");
                    String photo = el.hasAttribute("photo") ? el.getAttribute("photo") : null;
                    String nationality = el.hasAttribute("nationality") ? el.getAttribute("nationality") : null;
                    LocalDate dob = el.hasAttribute("dob") ? LocalDate.parse(el.getAttribute("dob")) : null;

                    PlayerStats stats = new PlayerStats();
                    NodeList statsNodes = el.getElementsByTagName("stats");
                    if (statsNodes.getLength() > 0) {
                        Element statsEl = (Element) statsNodes.item(0);
                        List<PlayerStats.MatchRecord> matches = new ArrayList<>();

                        NodeList matchNodes = statsEl.getElementsByTagName("match");
                        for (int j = 0; j < matchNodes.getLength(); j++) {
                            Element matchEl = (Element) matchNodes.item(j);
                            boolean won = "WON".equals(matchEl.getAttribute("result"));
                            double playtime = parseDouble(matchEl.getAttribute("playtime"), 0.0);
                            UUID oppId = UUID.fromString(matchEl.getAttribute("opponent-id"));
                            String oppName = matchEl.getAttribute("opponent-username");
                            matches.add(new PlayerStats.MatchRecord(won, playtime, oppId, oppName));
                        }
                        stats = new PlayerStats(matches);
                    }

                    User user = new User(userId, username, passwordHash, photo);
                    user.setNationality(nationality);
                    user.setDob(dob);
                    user.setStats(stats);

                    userStore.loadUser(user);
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

    @Override
    public void saveFrom(UserStore userStore) {
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
                userEl.setAttribute("id", user.getUserId().toString());
                userEl.setAttribute("username", user.getUsername());
                userEl.setAttribute("passwordHash", user.getPasswordHash());
                if (user.getPhoto() != null) userEl.setAttribute("photo", user.getPhoto());
                if (user.getNationality() != null) userEl.setAttribute("nationality", user.getNationality());
                if (user.getDob() != null) userEl.setAttribute("dob", user.getDob().toString());

                Element statsEl = doc.createElement("stats");
                for (PlayerStats.MatchRecord match : user.getStats().matches()) {
                    Element matchEl = doc.createElement("match");
                    matchEl.setAttribute("result", match.won() ? "WON" : "LOST");
                    matchEl.setAttribute("playtime", String.valueOf(match.playtimeSecs()));
                    matchEl.setAttribute("opponent-id", match.opponentId().toString());
                    matchEl.setAttribute("opponent-username", match.opponentUsername());
                    statsEl.appendChild(matchEl);
                }
                userEl.appendChild(statsEl);
                root.appendChild(userEl);
            }

            try {
                newValidator().validate(new DOMSource(doc));
            } catch (SAXException e) {
                logger.error("Generated users document failed schema validation — aborting save: {}", e.getMessage());
                return;
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
