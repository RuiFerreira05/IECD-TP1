import iecd.a51597.common.protocol.builders.XMLMessageBuilder;
import iecd.a51597.common.protocol.parsers.XMLParser;
import iecd.a51597.common.protocol.ProtocolConstants;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.game.GameManager;
import iecd.a51597.server.handlers.*;
import iecd.a51597.server.network.Connection;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.server.store.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests MessageHandler.dispatch() routing and pre-dispatch guards
 * (message type check, protocol version check).
 *
 * The Connection is mocked so that sendMessage() calls can be
 * captured and the resulting XML inspected.
 */
class MessageHandlerTest {

    private MessageHandler handler;
    private Connection conn;
    private XMLParser parser;

    private static final UUID MSG_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");
    private static final UUID SES_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440098");
    private static final String TS   = "2026-04-20T12:00:00Z";

    @BeforeEach
    void setUp() {
        ServerConfiguration.PROTOCOL_VERSION = "1.0";

        XMLMessageBuilder builder = new XMLMessageBuilder();
        parser = new XMLParser();
        UserStore userStore = new UserStore();
        SessionManager sessionManager = new SessionManager();
        GameManager gameManager = new GameManager();

        AuthHandler    auth    = new AuthHandler(builder, sessionManager, userStore);
        ProfileHandler profile = new ProfileHandler(builder, sessionManager, userStore);
        SearchHandler  search  = new SearchHandler(builder, userStore);
        GameHandler    game    = new GameHandler(builder, sessionManager, userStore, gameManager);

        handler = new MessageHandler(parser, builder, auth, profile, search, game);
        conn = mock(Connection.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Capture the byte[] passed to Connection.sendMessage(). */
    private String capturedXml() {
        var captor = ArgumentCaptor.forClass(byte[].class);
        verify(conn, times(1)).sendMessage(captor.capture());
        return new String(captor.getValue(), StandardCharsets.UTF_8);
    }

    private Document capturedDoc() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(capturedXml().getBytes(StandardCharsets.UTF_8))
        );
    }

    private String text(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent().trim();
    }

    private String errorCode(Document doc) {
        NodeList nodes = doc.getElementsByTagName("error");
        if (nodes.getLength() == 0) return null;
        return ((Element) nodes.item(0)).getAttribute("code");
    }

    private void assertError(Document doc, UUID expectedId, String expectedAction, ErrorCodeType expectedCode) {
        assertEquals("RESPONSE", doc.getDocumentElement().getAttribute("type"));
        assertEquals(expectedId.toString(), doc.getDocumentElement().getAttribute("id"));
        assertEquals(expectedAction, text(doc, "action"));
        assertEquals("ERROR", text(doc, "status"));
        assertEquals(expectedCode.name(), errorCode(doc));
    }

    private byte[] requestBytes(String type, UUID id, String version,
                                String action, String sessionLine, String body) {
        return ("""
                <?xml version="1.0" encoding="UTF-8"?>
                <message type="%s" id="%s" version="%s">
                    <header>
                        <action>%s</action>
                        %s
                        <timestamp>%s</timestamp>
                    </header>
                    <body>%s</body>
                </message>
                """.formatted(type, id, version, action, sessionLine, TS, body))
                .getBytes(StandardCharsets.UTF_8);
    }

    private byte[] request(String action, String body) {
        return requestBytes("REQUEST", MSG_ID, "1.0", action, "", body);
    }

    private byte[] requestWithSession(String action, String body) {
        return requestBytes("REQUEST", MSG_ID, "1.0", action,
                "<session>%s</session>".formatted(SES_ID), body);
    }

    // ── malformed frame ───────────────────────────────────────────────────────

    @Test
    void handle_notXML_sendsNoIdMalformedError() throws Exception {
        handler.handle("garbage bytes".getBytes(), conn);

        Document doc = capturedDoc();
        assertError(doc, ProtocolConstants.ERROR_NO_ID, "UNKNOWN", ErrorCodeType.MALFORMED_REQUEST);
    }

    // ── message type guard ────────────────────────────────────────────────────

    @Test
    void handle_responseType_sendsUnexpectedMessageTypeError() throws Exception {
        // Keep body parse-valid so MessageHandler reaches the message-type guard.
        byte[] bytes = requestBytes("RESPONSE", MSG_ID, "1.0", "GAME_OVER", "", "<status>OK</status>");
        handler.handle(bytes, conn);

        Document doc = capturedDoc();
        assertError(doc, MSG_ID, "GAME_OVER", ErrorCodeType.UNEXPECTED_MESSAGE_TYPE);
    }

    @Test
    void handle_pushType_sendsUnexpectedMessageTypeError() throws Exception {
        // PUSH + REGISTER → parsePushBody returns Unknown() without needing body fields
        byte[] bytes = requestBytes("PUSH", MSG_ID, "1.0", "REGISTER", "", "");
        handler.handle(bytes, conn);

        Document doc = capturedDoc();
        assertError(doc, MSG_ID, "REGISTER", ErrorCodeType.UNEXPECTED_MESSAGE_TYPE);
    }

    // ── protocol version guard ────────────────────────────────────────────────

    @Test
    void handle_wrongVersion_sendsOutdatedProtocolError() throws Exception {
        // Keep body parse-valid so MessageHandler reaches the protocol-version guard.
        byte[] bytes = requestBytes("REQUEST", MSG_ID, "9.9", "LOGIN", "",
                "<username>alice</username><password>secret</password>");
        handler.handle(bytes, conn);

        Document doc = capturedDoc();
        assertError(doc, MSG_ID, "LOGIN", ErrorCodeType.OUTDATED_PROTOCOL);
    }

    // ── UNKNOWN action ────────────────────────────────────────────────────────

    @Test
    void handle_unknownAction_sendsUnexpectedMessageActionError() throws Exception {
        handler.handle(request("UNKNOWN", ""), conn);

        Document doc = capturedDoc();
        assertError(doc, MSG_ID, "UNKNOWN", ErrorCodeType.UNEXPECTED_MESSAGE_ACTION);
    }

    // ── REGISTER routing ──────────────────────────────────────────────────────

    @Test
    void handle_registerNewUser_sendsOkResponse() throws Exception {
        handler.handle(request("REGISTER",
                "<username>newuser</username><password>pass</password>"), conn);

        Document doc = capturedDoc();
        assertEquals("RESPONSE", doc.getDocumentElement().getAttribute("type"));
        assertEquals(MSG_ID.toString(), doc.getDocumentElement().getAttribute("id"));
        assertEquals("REGISTER", text(doc, "action"));
        assertEquals("OK", text(doc, "status"));
    }

    @Test
    void handle_registerDuplicateUser_sendsUsernameTakenError() throws Exception {
        byte[] reg = request("REGISTER",
                "<username>alice</username><password>pass</password>");

        handler.handle(reg, conn);                // first registration → OK
        reset(conn);
        handler.handle(reg, conn);                // duplicate → error

        Document doc = capturedDoc();
        assertError(doc, MSG_ID, "REGISTER", ErrorCodeType.USERNAME_TAKEN);
    }

    // ── LOGIN routing ─────────────────────────────────────────────────────────

    @Test
    void handle_loginUnknownUser_sendsAuthFailedError() throws Exception {
        handler.handle(request("LOGIN",
                "<username>nobody</username><password>pass</password>"), conn);

        Document doc = capturedDoc();
        assertError(doc, MSG_ID, "LOGIN", ErrorCodeType.AUTH_FAILED);
    }

    @Test
    void handle_loginCorrectCredentials_sendsLoginSuccess() throws Exception {
        // Register first
        handler.handle(request("REGISTER",
                "<username>alice</username><password>secret</password>"), conn);
        reset(conn);

        // Login with correct password
        handler.handle(request("LOGIN",
                "<username>alice</username><password>secret</password>"), conn);

        Document doc = capturedDoc();
        assertEquals("LOGIN", text(doc, "action"));
        assertEquals("OK", text(doc, "status"));
        assertNotNull(text(doc, "session"));
        assertFalse(text(doc, "session").isBlank());
    }

    // ── SEARCH_USERS routing ──────────────────────────────────────────────────

    @Test
    void handle_searchUsers_sendsResults() throws Exception {
        handler.handle(request("SEARCH_USERS", "<query>nobody</query>"), conn);

        Document doc = capturedDoc();
        assertEquals("SEARCH_USERS", text(doc, "action"));
        assertEquals("OK", text(doc, "status"));
        assertEquals(1, doc.getElementsByTagName("results").getLength());
    }

    // ── GAME_OVER rejection ───────────────────────────────────────────────────

    @Test
    void handle_gameOverFromClient_sendsUnexpectedActionError() throws Exception {
        handler.handle(requestWithSession("GAME_OVER", ""), conn);

        Document doc = capturedDoc();
        assertError(doc, MSG_ID, "GAME_OVER", ErrorCodeType.UNEXPECTED_MESSAGE_ACTION);
    }

    // ── LOGOUT without session ────────────────────────────────────────────────

    @Test
    void handle_logoutWithoutSession_sendsNotAuthenticatedError() throws Exception {
        handler.handle(request("LOGOUT", ""), conn);

        Document doc = capturedDoc();
        assertError(doc, MSG_ID, "LOGOUT", ErrorCodeType.NOT_AUTHENTICATED);
    }
}
