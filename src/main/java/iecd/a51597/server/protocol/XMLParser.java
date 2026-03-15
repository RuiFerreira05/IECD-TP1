package iecd.a51597.server.protocol;

import iecd.a51597.server.protocol.errors.CommError;
import iecd.a51597.server.protocol.errors.MalformedMessageException;
import iecd.a51597.server.protocol.errors.MessageParseException;
import iecd.a51597.server.protocol.types.ActionType;
import iecd.a51597.server.protocol.types.BodyKey;
import iecd.a51597.server.protocol.types.MessageType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class XMLParser implements CommParser {

    DocumentBuilder builder;
    Validator validator;

    public XMLParser() throws ParserConfigurationException, SAXException {
        // Build the document builder
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setIgnoringComments(true);
            dbf.setNamespaceAware(true);
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ParserConfigurationException(e.getMessage());
        }

        // Build the schema validator
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(new File("src/main/resources/protocol.xsd"));
            validator = schema.newValidator();
        } catch (SAXException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public Message parseMessage(InputStream input) throws CommError {
        Document doc;

        try {
            doc = builder.parse(input);
        } catch (Exception e) {
            throw new MessageParseException("Failed to parse XML message: " + e.getMessage(), e);
        }

        if (validateMessage(doc)) {
            return createMessage(doc);
        } else {
            throw new MalformedMessageException("XML message does not conform to schema");
        }
    }

    private Message createMessage(Document doc) {
        Element root = doc.getDocumentElement();

        UUID messageId = UUID.fromString(root.getAttribute("id"));
        MessageType type = MessageType.fromString(root.getAttribute("type"));

        Element header = (Element) doc.getElementsByTagName("header").item(0);
        Element body = (Element) doc.getElementsByTagName("body").item(0);

        ActionType action = ActionType.fromString(getField(header, "action"));
        String sessionTokenRaw = getField(header, "sessionToken");

        Optional<UUID> sessionToken;
        if (sessionTokenRaw != null) {
            sessionToken = Optional.of(UUID.fromString(sessionTokenRaw));
        } else {
            sessionToken = Optional.empty();
        }

        Map<BodyKey, String> bodyMap = mapBodyFields(body);

        return new Message(messageId, type, action, sessionToken, bodyMap);
    }

    private Map<BodyKey, String> mapBodyFields(Element body) {
        Map<BodyKey, String> bodyMap = new java.util.HashMap<>();
        var fields = body.getChildNodes();
        for (int i = 0; i < fields.getLength(); i++) {
            if (fields.item(i) instanceof Element field) {
                String key = field.getTagName();
                String value = field.getTextContent().trim();
                BodyKey bodyKey = BodyKey.fromString(key);
                bodyMap.put(bodyKey, value);
            }
        }
        return bodyMap;
    }

    private String getField(Element rootElement, String tag) {
        var nodes = rootElement.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent().trim();
    }

    public boolean validateMessage(Document document) {
        try {
            validator.validate(new DOMSource(document));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
