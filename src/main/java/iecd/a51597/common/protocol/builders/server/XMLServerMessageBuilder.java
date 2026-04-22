package iecd.a51597.common.protocol.builders.server;

import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.common.store.PlayerStats;
import iecd.a51597.common.store.User;
import iecd.a51597.common.protocol.ProtocolConstants;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.common.protocol.types.MessageType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DOM-based {@link ServerMessageBuilder} implementation that emits XML protocol payloads.
 */
public class XMLServerMessageBuilder implements ServerMessageBuilder {

    private final DocumentBuilderFactory dbf;
    private final TransformerFactory tf;

    /**
     * Creates a builder with JAXP factories configured for namespace-aware documents.
     */
    public XMLServerMessageBuilder() {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        dbf.setNamespaceAware(true);
        tf = TransformerFactory.newInstance();
    }

    // ====== PRIVATE HELPERS ======

    private record MessageSkeleton(Document document, Element header, Element body) {}

    private MessageSkeleton getSkeleton(MessageType msgType, UUID id, ActionType actionType) {
        try {
            Document doc = dbf.newDocumentBuilder().newDocument();
            doc.setXmlStandalone(true);

            Element root = doc.createElement("message");
            root.setAttribute("type", msgType.name());
            root.setAttribute("id", id.toString());
            root.setAttribute("version", ServerConfiguration.PROTOCOL_VERSION);
            doc.appendChild(root);

            Element header = doc.createElement("header");
            root.appendChild(header);

            Element action = doc.createElement("action");
            action.setTextContent(actionType.name());
            header.appendChild(action);

            Element timestamp = doc.createElement("timestamp");
            timestamp.setTextContent(Instant.now().toString());
            header.appendChild(timestamp);

            Element body = doc.createElement("body");
            root.appendChild(body);

            return new MessageSkeleton(doc, header, body);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("DocumentBuilder unavailable — check JAXP classpath configuration", e);
        }
    }

    private byte[] serialize(Document doc) {
        try {
            Transformer transformer = tf.newTransformer();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toByteArray();
        } catch (TransformerException e) {
            throw new IllegalStateException("Failed to serialize XML document", e);
        }
    }

    private Element textElement(Document doc, String tag, String text) {
        Element e = doc.createElement(tag);
        e.setTextContent(text);
        return e;
    }

    private Element userElement(Document doc, User user) {
        Element userEl = doc.createElement("user");
        userEl.appendChild(textElement(doc, "id",       user.getUserId().toString()));
        userEl.appendChild(textElement(doc, "username", user.getUsername()));
        if (user.getPhoto() != null) {
            userEl.appendChild(textElement(doc, "photo", user.getPhoto()));
        }
        if (user.getNationality() != null) {
            userEl.appendChild(textElement(doc, "nationality", user.getNationality()));
        }
        if (user.getDob() != null) {
            userEl.appendChild(textElement(doc, "dob", user.getDob().toString()));
        }
        userEl.appendChild(playerStatsElement(doc, user.getStats()));

        return userEl;
    }

    private Element playerStatsElement(Document doc, PlayerStats stats) {
        Element statsEl = doc.createElement("stats");
        for (PlayerStats.MatchRecord match : stats.matches()) {
            Element matchEl = doc.createElement("match");
            matchEl.setAttribute("result", match.won() ? "WON" : "LOST");
            matchEl.setAttribute("playtime", String.valueOf(match.playtimeSecs()));
            matchEl.setAttribute("opponent-id", match.opponentId().toString());
            matchEl.setAttribute("opponent-username", match.opponentUsername());
            statsEl.appendChild(matchEl);
        }

        return statsEl;
    }

    // ====== GENERIC ======

    /**
     * Creates a generic error payload for malformed frames with no parsed id.
     */
    @Override
    public byte[] errorNoId(ErrorCodeType errorCode, String description) {
        return error(ProtocolConstants.ERROR_NO_ID, ActionType.UNKNOWN, errorCode, description);
    }

    /**
     * Creates a correlated error response payload.
     */
    @Override
    public byte[] error(UUID messageId, ActionType actionType, ErrorCodeType errorCode, String description) {
        MessageSkeleton s = getSkeleton(MessageType.RESPONSE, messageId, actionType);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "status", "ERROR"));

        Element error = doc.createElement("error");
        error.setAttribute("code", errorCode.name());
        error.setTextContent(description);
        body.appendChild(error);

        return serialize(doc);
    }

    /**
     * Creates a correlated success response payload.
     */
    @Override
    public byte[] ok(UUID messageId, ActionType actionType) {
        MessageSkeleton s = getSkeleton(MessageType.RESPONSE, messageId, actionType);
        s.body().appendChild(textElement(s.document(), "status", "OK"));
        return serialize(s.document());
    }

    // ====== AUTH ======

    /**
     * Creates a successful login response payload containing session and user profile data.
     */
    @Override
    public byte[] loginSuccess(UUID messageId, UUID sessionToken, User user) {
        MessageSkeleton s = getSkeleton(MessageType.RESPONSE, messageId, ActionType.LOGIN);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "status",  "OK"));
        body.appendChild(textElement(doc, "session", sessionToken.toString()));
        body.appendChild(userElement(doc, user));

        return serialize(doc);
    }

    // ====== SEARCH ======

    /**
     * Creates a successful search response payload with all matching users.
     */
    @Override
    public byte[] searchUsersSuccess(UUID messageId, List<User> results) {
        MessageSkeleton s = getSkeleton(MessageType.RESPONSE, messageId, ActionType.SEARCH_USERS);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "status", "OK"));

        Element resultsEl = doc.createElement("results");
        for (User u : results) {
            resultsEl.appendChild(userElement(doc, u));
        }
        body.appendChild(resultsEl);

        return serialize(doc);
    }

    // ====== GAME ======

    /**
     * Creates an invitation acknowledgement for the inviter.
     */
    @Override
    public byte[] gameInviteResponse(UUID messageId, UUID gameId) {
        MessageSkeleton s = getSkeleton(MessageType.RESPONSE, messageId, ActionType.GAME_INVITE);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "status",  "OK"));
        body.appendChild(textElement(doc, "game-id", gameId.toString()));

        return serialize(doc);
    }

    /**
     * Creates a push payload for the invited user.
     */
    @Override
    public byte[] gameInvitePush(UUID gameId, User fromUser) {
        MessageSkeleton s = getSkeleton(MessageType.PUSH, UUID.randomUUID(), ActionType.GAME_INVITE);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "from-user-id",  fromUser.getUserId().toString()));
        body.appendChild(textElement(doc, "from-username", fromUser.getUsername()));
        body.appendChild(textElement(doc, "game-id",       gameId.toString()));

        return serialize(doc);
    }

    /**
     * Creates a push payload indicating that an invitation was accepted.
     */
    @Override
    public byte[] gameInviteAcceptedPush(UUID gameId, User opponent) {
        MessageSkeleton s = getSkeleton(MessageType.PUSH, UUID.randomUUID(), ActionType.GAME_INVITE_RESPONSE);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "game-id",           gameId.toString()));
        body.appendChild(textElement(doc, "accepted",          "true"));
        body.appendChild(textElement(doc, "opponent-username", opponent.getUsername()));

        return serialize(doc);
    }

    /**
     * Creates a push payload indicating that an invitation was declined.
     */
    @Override
    public byte[] gameInviteDeclinedPush(UUID gameId) {
        MessageSkeleton s = getSkeleton(MessageType.PUSH, UUID.randomUUID(), ActionType.GAME_INVITE_RESPONSE);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "game-id",  gameId.toString()));
        body.appendChild(textElement(doc, "accepted", "false"));

        return serialize(doc);
    }

    /**
     * Creates a push payload propagating an accepted move.
     */
    @Override
    public byte[] gameMovePush(UUID gameId, String rawMove) {
        MessageSkeleton s = getSkeleton(MessageType.PUSH, UUID.randomUUID(), ActionType.GAME_MOVE);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "game-id", gameId.toString()));

        Element moveEl = doc.createElement("move");
        moveEl.appendChild(doc.createCDATASection(rawMove));
        body.appendChild(moveEl);

        return serialize(doc);
    }

    /**
     * Creates a game-over push payload.
     */
    @Override
    public byte[] gameOverPush(UUID gameId, User winner) {
        MessageSkeleton s = getSkeleton(MessageType.PUSH, UUID.randomUUID(), ActionType.GAME_OVER);
        Document doc = s.document();
        Element body = s.body();

        body.appendChild(textElement(doc, "game-id",         gameId.toString()));
        body.appendChild(textElement(doc, "winner-id",       winner.getUserId().toString()));
        body.appendChild(textElement(doc, "winner-username", winner.getUsername()));

        return serialize(doc);
    }
}