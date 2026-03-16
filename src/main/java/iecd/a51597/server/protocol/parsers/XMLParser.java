package iecd.a51597.server.protocol.parsers;

import iecd.a51597.server.protocol.Message;
import iecd.a51597.server.protocol.exceptions.CommException;
import iecd.a51597.server.protocol.exceptions.MalformedMessageException;
import iecd.a51597.server.protocol.exceptions.MessageParseException;
import iecd.a51597.server.protocol.types.ActionType;
import iecd.a51597.server.protocol.types.BodyKey;
import iecd.a51597.server.protocol.types.MessageType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XMLParser implements CommParser {

    private final Schema schema;
    private final DocumentBuilderFactory dbf;

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

    private Message createMessage(Document doc) {
        Element root = doc.getDocumentElement();

        UUID messageId = UUID.fromString(root.getAttribute("id"));
        MessageType type = MessageType.fromString(root.getAttribute("type"));
        String version = root.getAttribute("version");

        Element header = (Element) doc.getElementsByTagName("header").item(0);
        Element body = (Element) doc.getElementsByTagName("body").item(0);

        String actionRaw = getField(header, "action");
        ActionType action;
        if (actionRaw != null) {
            action = ActionType.fromString(actionRaw);
        } else {
            action = ActionType.UNKNOWN;
        }
        String sessionTokenRaw = getField(header, "session");

        UUID sessionToken;
        if (sessionTokenRaw != null) {
            sessionToken = UUID.fromString(sessionTokenRaw);
        } else {
            sessionToken = null;
        }

        Map<BodyKey, String> bodyMap = mapBodyFields(body);

        return new Message(messageId, type, version, action, sessionToken, bodyMap);
    }

    private Map<BodyKey, String> mapBodyFields(Element body) {
        Map<BodyKey, String> bodyMap = new HashMap<>();
        NodeList fields = body.getChildNodes();
        for (int i = 0; i < fields.getLength(); i++) {
            if (fields.item(i) instanceof Element field) {
                String key = field.getTagName();
                String value = field.getTextContent().trim();
                BodyKey bodyKey = BodyKey.fromString(key);
                if (bodyKey != null) bodyMap.put(bodyKey, value);
            }
        }
        return bodyMap;
    }

    private String getField(Element rootElement, String tag) {
        NodeList nodes = rootElement.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent().trim();
    }

    private void validateMessage(Document document, Validator validator) throws MalformedMessageException {
        try {
            validator.validate(new DOMSource(document));
        } catch (Exception e) {
            throw new MalformedMessageException("XML message does not conform to schema: " + e.getMessage(), e);
        }
    }
}
