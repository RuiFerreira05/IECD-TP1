package iecd.a51597.common.protocol.builders.client;

import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class XMLClientMessageBuilderTest {

    private XMLClientMessageBuilder builder;
    private static final UUID MSG_ID = UUID.randomUUID();
    private static final UUID SES_ID = UUID.randomUUID();
    private static final UUID GAME_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ClientConfiguration.PROTOCOL_VERSION = "1.0";
        builder = new XMLClientMessageBuilder();
    }

    private Document parse(byte[] bytes) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private String text(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent().trim();
    }

    @Test
    void register_messageStructure() throws Exception {
        byte[] bytes = builder.register("alice", "pass");
        Document doc = parse(bytes);

        assertEquals("REQUEST", doc.getDocumentElement().getAttribute("type"));
        assertEquals("REGISTER", text(doc, "action"));
        assertEquals("alice", text(doc, "username"));
        assertEquals("pass", text(doc, "password"));
    }

    @Test
    void login_messageStructure() throws Exception {
        byte[] bytes = builder.login("bob", "secret");
        Document doc = parse(bytes);

        assertEquals("LOGIN", text(doc, "action"));
        assertEquals("bob", text(doc, "username"));
        assertEquals("secret", text(doc, "password"));
    }

    @Test
    void logout_includesSession() throws Exception {
        byte[] bytes = builder.logout(SES_ID);
        Document doc = parse(bytes);

        assertEquals("LOGOUT", text(doc, "action"));
        assertEquals(SES_ID.toString(), text(doc, "session"));
    }

    @Test
    void updateProfile_partialUpdate() throws Exception {
        byte[] bytes = builder.updateProfile(SES_ID, "newname", null, null, "PT", null);
        Document doc = parse(bytes);

        assertEquals("newname", text(doc, "username"));
        assertEquals("PT", text(doc, "nationality"));
        assertEquals(0, doc.getElementsByTagName("password").getLength());
        assertEquals(0, doc.getElementsByTagName("photo").getLength());
        assertEquals(0, doc.getElementsByTagName("dob").getLength());
    }

    @Test
    void gameInvite_structure() throws Exception {
        byte[] bytes = builder.gameInvite(SES_ID, TARGET_ID);
        Document doc = parse(bytes);

        assertEquals("GAME_INVITE", text(doc, "action"));
        assertEquals(TARGET_ID.toString(), text(doc, "target-user-id"));
    }

    @Test
    void gameMove_includesCDATA() throws Exception {
        byte[] bytes = builder.gameMove(SES_ID, GAME_ID, "0,0,1,0");
        Document doc = parse(bytes);

        assertEquals("GAME_MOVE", text(doc, "action"));
        assertEquals(GAME_ID.toString(), text(doc, "game-id"));
        assertEquals("0,0,1,0", text(doc, "move"));
    }
}
