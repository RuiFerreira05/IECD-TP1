package iecd.a51597.server.protocol.builders;

import iecd.a51597.server.User;
import iecd.a51597.server.protocol.ProtocolConstants;
import iecd.a51597.server.protocol.types.ActionType;
import iecd.a51597.server.protocol.types.ErrorCodeType;
import iecd.a51597.server.protocol.types.MessageType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class XMLMessageBuilder implements MessageBuilder {

    private final DocumentBuilderFactory dbf;

    public XMLMessageBuilder() {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        dbf.setNamespaceAware(true);
    }

    private record MessageSkeleton(Document document, Element header, Element body) {}

    private MessageSkeleton getSkeleton(MessageType msgType, UUID id, ActionType actionType) throws ParserConfigurationException {
        Document doc = dbf.newDocumentBuilder().newDocument();
        doc.setXmlStandalone(true);

        Element root = doc.createElement("message");
        root.setAttribute("type", msgType.name());
        root.setAttribute("id", id.toString());
        root.setAttribute("version", ProtocolConstants.PROTOCOL_VERSION);
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
    }

    @Override
    public byte[] errorNoId(ErrorCodeType errorCode, String description) throws ParserConfigurationException {
        return error(ProtocolConstants.ERROR_NO_ID, ActionType.UNKNOWN, errorCode, description);
    }

    @Override
    public byte[] error(UUID messageId, ActionType actionType, ErrorCodeType errorCode, String description) throws ParserConfigurationException {
        return null; //TODO
    }

    @Override
    public byte[] ok(UUID messageId) {
        return null; //TODO
    }

    @Override
    public byte[] loginSuccess(UUID messageId, UUID sessionToken, User user) {
        return null; //TODO
    }

    @Override
    public byte[] searchUsersSuccess(UUID messageId, List<User> results) {
        return null; //TODO
    }

    @Override
    public byte[] gameInviteResponse(UUID messageId, UUID gameId) {
        return null; //TODO
    }

    @Override
    public byte[] gameInvitePush(UUID gameId, User fromUser) {
        return null; //TODO
    }

    @Override
    public byte[] gameInviteAcceptedPush(UUID gameId, User user) {
        return null; //TODO
    }

    @Override
    public byte[] gameInviteDeclinedPush(UUID gameId) {
        return null; //TODO
    }

    @Override
    public byte[] gameMovePush(UUID gameId, String movePayload) {
        return null; //TODO
    }

    @Override
    public byte[] gameOverPush(UUID gameId, User winner) {
        return null; //TODO
    }
}
