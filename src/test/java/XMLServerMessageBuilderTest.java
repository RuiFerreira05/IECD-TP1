import iecd.a51597.common.protocol.ProtocolConstants;
import iecd.a51597.common.protocol.builders.server.XMLServerMessageBuilder;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.common.store.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class XMLServerMessageBuilderTest {

    private XMLServerMessageBuilder builder;
    private static final UUID MSG_ID  = UUID.randomUUID();
    private static final UUID SES_ID  = UUID.randomUUID();
    private static final UUID GAME_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ServerConfiguration.PROTOCOL_VERSION = "1.0";
        builder = new XMLServerMessageBuilder();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    private String attr(Document doc, String tag, String attrName) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return ((Element) nodes.item(0)).getAttribute(attrName);
    }

    private User makeUser(String name, boolean withOptional) {
        UUID id = UUID.randomUUID();
        User u = new User(id, name, "hash", "photo_data");
        if (withOptional) {
            u.setNationality("PT");
            u.setDob(LocalDate.of(2000, 1, 1));
        }
        return u;
    }

    // ── errorNoId ────────────────────────────────────────────────────────────

    @Test
    void errorNoId_containsSentinelIdAndUnknownAction() throws Exception {
        byte[] bytes = builder.errorNoId(ErrorCodeType.INTERNAL_ERROR, "boom");
        Document doc = parse(bytes);

        assertEquals(ProtocolConstants.ERROR_NO_ID.toString(),
                doc.getDocumentElement().getAttribute("id"));
        assertEquals("UNKNOWN", text(doc, "action"));
        assertEquals("ERROR", text(doc, "status"));
        assertEquals("INTERNAL_ERROR", attr(doc, "error", "code"));
    }

    // ── error ─────────────────────────────────────────────────────────────────

    @Test
    void error_echoesMessageIdAndAction() throws Exception {
        byte[] bytes = builder.error(MSG_ID, ActionType.LOGIN, ErrorCodeType.AUTH_FAILED, "bad creds");
        Document doc = parse(bytes);

        assertEquals(MSG_ID.toString(), doc.getDocumentElement().getAttribute("id"));
        assertEquals("RESPONSE",    doc.getDocumentElement().getAttribute("type"));
        assertEquals("LOGIN",        text(doc, "action"));
        assertEquals("AUTH_FAILED",  attr(doc, "error", "code"));
        assertEquals("bad creds",    text(doc, "error"));
    }

    // ── ok ───────────────────────────────────────────────────────────────────

    @Test
    void ok_containsStatusOK() throws Exception {
        byte[] bytes = builder.ok(MSG_ID, ActionType.LOGOUT);
        Document doc = parse(bytes);

        assertEquals("RESPONSE", doc.getDocumentElement().getAttribute("type"));
        assertEquals("OK", text(doc, "status"));
    }

    // ── loginSuccess ──────────────────────────────────────────────────────────

    @Test
    void loginSuccess_containsSessionTokenAndUsername() throws Exception {
        User alice = makeUser("alice", true);
        byte[] bytes = builder.loginSuccess(MSG_ID, SES_ID, alice);
        Document doc = parse(bytes);

        assertEquals("OK",            text(doc, "status"));
        assertEquals(SES_ID.toString(), text(doc, "session"));
        assertEquals("alice",          text(doc, "username"));
        assertEquals(alice.getUserId().toString(), text(doc, "id"));
    }

    @Test
    void loginSuccess_nullNationality_elementAbsent() throws Exception {
        // user without nationality or dob — must not throw NPE (the critical bug fix)
        User user = new User(UUID.randomUUID(), "nobody", "hash", null);

        assertDoesNotThrow(() -> {
            byte[] bytes = builder.loginSuccess(MSG_ID, SES_ID, user);
            Document doc = parse(bytes);
            // nationality element should be absent
            assertEquals(0, doc.getElementsByTagName("nationality").getLength());
            assertEquals(0, doc.getElementsByTagName("dob").getLength());
        });
    }

    @Test
    void loginSuccess_withOptionalFields_includesNationalityAndDob() throws Exception {
        User alice = makeUser("alice", true);
        byte[] bytes = builder.loginSuccess(MSG_ID, SES_ID, alice);
        Document doc = parse(bytes);

        assertEquals("PT", text(doc, "nationality"));
        assertEquals("2000-01-01", text(doc, "dob"));
    }

    @Test
    void loginSuccess_passwordHashNotIncluded() throws Exception {
        User alice = makeUser("alice", false);
        byte[] bytes = builder.loginSuccess(MSG_ID, SES_ID, alice);
        String xml = new String(bytes);

        assertFalse(xml.contains("passwordHash"));
        assertFalse(xml.contains("hash")); // the actual hash value
    }

    // ── searchUsersSuccess ────────────────────────────────────────────────────

    @Test
    void searchUsersSuccess_emptyList_resultsElementPresent() throws Exception {
        byte[] bytes = builder.searchUsersSuccess(MSG_ID, List.of());
        Document doc = parse(bytes);

        assertEquals("OK", text(doc, "status"));
        assertEquals(1, doc.getElementsByTagName("results").getLength());
    }

    @Test
    void searchUsersSuccess_multipleUsers_allIncluded() throws Exception {
        User alice = makeUser("alice", false);
        User bob   = makeUser("bob",   false);
        byte[] bytes = builder.searchUsersSuccess(MSG_ID, List.of(alice, bob));
        Document doc = parse(bytes);

        assertEquals(2, doc.getElementsByTagName("user").getLength());

        Set<String> usernames = new HashSet<>();
        NodeList users = doc.getElementsByTagName("user");
        for (int i = 0; i < users.getLength(); i++) {
            Element userEl = (Element) users.item(i);
            usernames.add(userEl.getElementsByTagName("username").item(0).getTextContent().trim());
        }
        assertEquals(Set.of("alice", "bob"), usernames);
    }

    // ── gameInviteResponse ────────────────────────────────────────────────────

    @Test
    void gameInviteResponse_containsGameId() throws Exception {
        byte[] bytes = builder.gameInviteResponse(MSG_ID, GAME_ID);
        Document doc = parse(bytes);

        assertEquals("OK",             text(doc, "status"));
        assertEquals(GAME_ID.toString(), text(doc, "game-id"));
    }

    // ── gameInvitePush ────────────────────────────────────────────────────────

    @Test
    void gameInvitePush_isPushTypeAndContainsFromUser() throws Exception {
        User alice = makeUser("alice", false);
        byte[] bytes = builder.gameInvitePush(GAME_ID, alice);
        Document doc = parse(bytes);

        assertEquals("PUSH",  doc.getDocumentElement().getAttribute("type"));
        assertEquals("alice", text(doc, "from-username"));
        assertEquals(alice.getUserId().toString(), text(doc, "from-user-id"));
        assertEquals(GAME_ID.toString(), text(doc, "game-id"));
    }

    // ── gameInviteAcceptedPush / DeclinedPush ─────────────────────────────────

    @Test
    void gameInviteAcceptedPush_acceptedTrueAndUsername() throws Exception {
        User bob = makeUser("bob", false);
        byte[] bytes = builder.gameInviteAcceptedPush(GAME_ID, bob);
        Document doc = parse(bytes);

        assertEquals("true", text(doc, "accepted"));
        assertEquals("bob",  text(doc, "opponent-username"));
    }

    @Test
    void gameInviteDeclinedPush_acceptedFalse() throws Exception {
        byte[] bytes = builder.gameInviteDeclinedPush(GAME_ID);
        Document doc = parse(bytes);

        assertEquals("false", text(doc, "accepted"));
    }

    // ── gameMovePush ──────────────────────────────────────────────────────────

    @Test
    void gameMovePush_wrapsRawMoveInCDATA() throws Exception {
        String raw = "1,2,3 & <special>";
        byte[] bytes = builder.gameMovePush(GAME_ID, raw);
        Document doc = parse(bytes);

        assertEquals(GAME_ID.toString(), text(doc, "game-id"));
        // getTextContent extracts CDATA text correctly
        assertEquals(raw, text(doc, "move"));
    }

    @Test
    void gameMovePush_isPushType() throws Exception {
        byte[] bytes = builder.gameMovePush(GAME_ID, "move");
        Document doc = parse(bytes);

        assertEquals("PUSH",      doc.getDocumentElement().getAttribute("type"));
        assertEquals("GAME_MOVE", text(doc, "action"));
    }

    // ── gameOverPush ──────────────────────────────────────────────────────────

    @Test
    void gameOverPush_containsWinnerInfo() throws Exception {
        User alice = makeUser("alice", false);
        byte[] bytes = builder.gameOverPush(GAME_ID, alice);
        Document doc = parse(bytes);

        assertEquals("PUSH",     doc.getDocumentElement().getAttribute("type"));
        assertEquals("GAME_OVER", text(doc, "action"));
        assertEquals(alice.getUserId().toString(), text(doc, "winner-id"));
        assertEquals("alice", text(doc, "winner-username"));
    }

    // ── protocol version ─────────────────────────────────────────────────────

    @Test
    void allMessages_carryProtocolVersion() throws Exception {
        byte[] bytes = builder.ok(MSG_ID, ActionType.LOGOUT);
        Document doc = parse(bytes);

        assertEquals("1.0", doc.getDocumentElement().getAttribute("version"));
    }

    // ── stats serialization ───────────────────────────────────────────────────

    @Test
    void loginSuccess_statsIncluded() throws Exception {
        User alice = makeUser("alice", false);
        alice.setStats(alice.getStats()
                .withMatch(true, 120.0, UUID.randomUUID(), "bob"));

        byte[] bytes = builder.loginSuccess(MSG_ID, SES_ID, alice);
        Document doc = parse(bytes);

        NodeList matches = doc.getElementsByTagName("match");
        assertEquals(1, matches.getLength());
        assertEquals("WON", ((Element) matches.item(0)).getAttribute("result"));
        assertEquals("120.0", ((Element) matches.item(0)).getAttribute("playtime"));
    }
}
