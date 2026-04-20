package iecd.a51597.common.protocol.parsers;

import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.exceptions.CommException;
import iecd.a51597.common.protocol.exceptions.MalformedMessageException;
import iecd.a51597.common.protocol.exceptions.MessageParseException;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.MessageType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.util.UUID;

/**
 * XML implementation of {@link CommParser} backed by {@code protocol.xsd} validation.
 */
public class XMLParser implements CommParser {

    private final Schema schema;
    private final DocumentBuilderFactory dbf;

    /**
     * Creates a parser and loads protocol schema resources from the classpath.
     */
    public XMLParser() {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = sf.newSchema(getClass().getResource("/protocol.xsd"));
        } catch (SAXException e) {
            throw new IllegalStateException("Failed to load protocol.xsd — ensure it is on the classpath", e);
        }

        dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        dbf.setNamespaceAware(true);
    }

    private DocumentBuilder getNewBuilder() {
        try {
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("DocumentBuilder unavailable — check JAXP classpath configuration", e);
        }
    }

    private Validator getNewValidator() {
        return schema.newValidator();
    }

    /**
     * Parses and validates a single XML protocol message.
     *
     * @param input input stream with XML payload bytes
     * @return parsed immutable message
     * @throws CommException when parsing or schema validation fails
     */
    @Override
    public Message parseMessage(InputStream input) throws CommException {

        Validator validator = getNewValidator();
        DocumentBuilder builder;
        builder = getNewBuilder();

        Document doc;
        try {
            doc = builder.parse(input);
        } catch (Exception e) {
            throw new MessageParseException("Failed to parse XML message: " + e.getMessage(), e);
        }

        validateMessage(doc, validator);

        return createMessage(doc);
    }

    /**
     * Materializes a protocol message instance from a validated XML document.
     */
    private Message createMessage(Document doc) throws MalformedMessageException {
        Element root = doc.getDocumentElement();

        UUID messageId = UUID.fromString(root.getAttribute("id"));
        MessageType type = MessageType.fromString(root.getAttribute("type"));
        String version = root.getAttribute("version");

        Element header = (Element) doc.getElementsByTagName("header").item(0);
        Element body = (Element) doc.getElementsByTagName("body").item(0);

        String actionRaw = getField(header, "action");
        ActionType action = actionRaw != null ? ActionType.fromString(actionRaw) : ActionType.UNKNOWN;
        if (action == null) action = ActionType.UNKNOWN;

        String sessionTokenRaw = getField(header, "session");
        UUID sessionToken = sessionTokenRaw != null ? UUID.fromString(sessionTokenRaw) : null;

        return new Message(messageId, type, version, action, sessionToken, parseBody(action, body));
    }

    /**
     * Parses action-specific payload fields from the body element.
     */
    private MessageBody parseBody(ActionType action, Element body) throws MalformedMessageException {
        return switch (action) {
            case REGISTER -> new MessageBody.Register(require(body, "username"), require(body, "password"));
            case LOGIN -> new MessageBody.Login(require(body, "username"), require(body, "password"));
            case LOGOUT -> new MessageBody.Logout();
            case UPDATE_PROFILE -> new MessageBody.UpdateProfile(getField(body, "username"), getField(body, "password"), getField(body, "photo"));
            case SEARCH_USERS -> new MessageBody.SearchUsers(require(body, "query"));
            case GAME_INVITE -> new MessageBody.GameInvite(requireUUID(body, "target-user-id"));
            case GAME_INVITE_RESPONSE -> new MessageBody.GameInviteResponse(requireUUID(body, "game-id"), Boolean.parseBoolean(require(body, "accept")));
            case GAME_MOVE -> new MessageBody.GameMove(requireUUID(body, "game-id"), requireElement(body, "move").getTextContent());
            case GAME_OVER -> new MessageBody.GameOver(null, null, null); // not supposed to happen
            case UNKNOWN -> new MessageBody.Unknown();
        };
    }

    /**
     * Reads a required textual field.
     */
    private String require(Element parent, String tag) throws MalformedMessageException {
        String value = getField(parent, tag);
        if (value == null)
            throw new MalformedMessageException("Missing required field: <" + tag + ">");
        return value;
    }

    /**
     * Reads a required UUID field.
     */
    private UUID requireUUID(Element parent, String tag) throws MalformedMessageException {
        try {
            return UUID.fromString(require(parent, tag));
        } catch (IllegalArgumentException e) {
            throw new MalformedMessageException("Invalid UUID in field <" + tag + ">", e);
        }
    }

    /**
     * Reads a required child element.
     */
    private Element requireElement(Element parent, String tag) throws MalformedMessageException {
        var nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0)
            throw new MalformedMessageException("Missing required element: <" + tag + ">");
        return (Element) nodes.item(0);
    }

    /**
     * Reads the first matching child tag text, or {@code null} when absent.
     */
    private String getField(Element rootElement, String tag) {
        NodeList nodes = rootElement.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent().trim();
    }

    /**
     * Validates an XML document against the protocol schema.
     */
    private void validateMessage(Document document, Validator validator) throws MalformedMessageException {
        try {
            validator.validate(new DOMSource(document));
        } catch (Exception e) {
            throw new MalformedMessageException("XML message does not conform to schema: " + e.getMessage(), e);
        }
    }
}
