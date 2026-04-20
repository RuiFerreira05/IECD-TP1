import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.exceptions.MalformedMessageException;
import iecd.a51597.common.protocol.exceptions.MessageParseException;
import iecd.a51597.common.protocol.parsers.XMLParser;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests XMLParser against valid REQUEST messages and expected error cases.
 * The parser is used server-side to parse client REQUEST messages only.
 */
class XMLParserTest {

    private XMLParser parser;

    private static final UUID MSG_ID  = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID SES_ID  = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID GAME_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
    private static final String TS    = "2026-04-20T12:00:00Z";

    @BeforeEach
    void setUp() {
        parser = new XMLParser();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Message parse(String xml) throws Exception {
        return parser.parseMessage(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String request(UUID id, String action, String sessionLine, String body) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <message type="REQUEST" id="%s" version="1.0">
                    <header>
                        <action>%s</action>
                        %s
                        <timestamp>%s</timestamp>
                    </header>
                    <body>%s</body>
                </message>
                """.formatted(id, action, sessionLine, TS, body);
    }

    private String noSession()            { return ""; }
    private String session(UUID token)    { return "<session>%s</session>".formatted(token); }

    // ── envelope fields ───────────────────────────────────────────────────────

    @Test
    void parseEnvelope_extractsIdTypeVersion() throws Exception {
        String xml = request(MSG_ID, "LOGOUT", session(SES_ID), "");
        Message msg = parse(xml);

        assertEquals(MSG_ID,          msg.messageId());
        assertEquals(MessageType.REQUEST, msg.messageType());
        assertEquals("1.0",           msg.version());
    }

    @Test
    void parseEnvelope_sessionTokenExtracted() throws Exception {
        String xml = request(MSG_ID, "LOGOUT", session(SES_ID), "");
        Message msg = parse(xml);

        assertEquals(SES_ID, msg.sessionToken());
    }

    @Test
    void parseEnvelope_noSession_tokenIsNull() throws Exception {
        String xml = request(MSG_ID, "REGISTER", noSession(),
                "<username>alice</username><password>pass</password>");
        Message msg = parse(xml);

        assertNull(msg.sessionToken());
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    @Test
    void parseRegisterRequest() throws Exception {
        String xml = request(MSG_ID, "REGISTER", noSession(),
                "<username>alice</username><password>secret</password>");
        Message msg = parse(xml);

        assertEquals(ActionType.REGISTER, msg.actionType());
        MessageBody.Register body = (MessageBody.Register) msg.body();
        assertEquals("alice",  body.username());
        assertEquals("secret", body.password());
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    @Test
    void parseLoginRequest() throws Exception {
        String xml = request(MSG_ID, "LOGIN", noSession(),
                "<username>bob</username><password>p4ss</password>");
        Message msg = parse(xml);

        assertEquals(ActionType.LOGIN, msg.actionType());
        MessageBody.LoginRequest body = (MessageBody.LoginRequest) msg.body();
        assertEquals("bob",  body.username());
        assertEquals("p4ss", body.password());
    }

    // ── LOGOUT ───────────────────────────────────────────────────────────────

    @Test
    void parseLogoutRequest_emptyBody() throws Exception {
        String xml = request(MSG_ID, "LOGOUT", session(SES_ID), "");
        Message msg = parse(xml);

        assertEquals(ActionType.LOGOUT, msg.actionType());
        assertInstanceOf(MessageBody.Logout.class, msg.body());
    }

    // ── UPDATE_PROFILE ────────────────────────────────────────────────────────

    @Test
    void parseUpdateProfile_usernameOnly() throws Exception {
        String xml = request(MSG_ID, "UPDATE_PROFILE", session(SES_ID),
                "<username>alice_new</username>");
        Message msg = parse(xml);

        assertEquals(ActionType.UPDATE_PROFILE, msg.actionType());
        MessageBody.UpdateProfile body = (MessageBody.UpdateProfile) msg.body();
        assertEquals("alice_new", body.username());
        assertNull(body.password());
        assertNull(body.photo());
    }

    @Test
    void parseUpdateProfile_allFields() throws Exception {
        String xml = request(MSG_ID, "UPDATE_PROFILE", session(SES_ID),
                "<username>x</username><password>np</password><photo>ph</photo>");
        Message msg = parse(xml);

        MessageBody.UpdateProfile body = (MessageBody.UpdateProfile) msg.body();
        assertEquals("x",  body.username());
        assertEquals("np", body.password());
        assertEquals("ph", body.photo());
    }

    // ── SEARCH_USERS ──────────────────────────────────────────────────────────

    @Test
    void parseSearchUsersRequest() throws Exception {
        String xml = request(MSG_ID, "SEARCH_USERS", noSession(), "<query>ali</query>");
        Message msg = parse(xml);

        assertEquals(ActionType.SEARCH_USERS, msg.actionType());
        MessageBody.SearchUsersRequest body = (MessageBody.SearchUsersRequest) msg.body();
        assertEquals("ali", body.query());
    }

    // ── GAME_INVITE ───────────────────────────────────────────────────────────

    @Test
    void parseGameInviteRequest() throws Exception {
        String xml = request(MSG_ID, "GAME_INVITE", session(SES_ID),
                "<target-user-id>%s</target-user-id>".formatted(USER_ID));
        Message msg = parse(xml);

        assertEquals(ActionType.GAME_INVITE, msg.actionType());
        MessageBody.GameInviteRequest body = (MessageBody.GameInviteRequest) msg.body();
        assertEquals(USER_ID, body.targetUserId());
    }

    // ── GAME_INVITE_RESPONSE ──────────────────────────────────────────────────

    @Test
    void parseGameInviteResponse_accept() throws Exception {
        String xml = request(MSG_ID, "GAME_INVITE_RESPONSE", session(SES_ID),
                "<game-id>%s</game-id><accept>true</accept>".formatted(GAME_ID));
        Message msg = parse(xml);

        assertEquals(ActionType.GAME_INVITE_RESPONSE, msg.actionType());
        MessageBody.GameInviteResponseRequest body = (MessageBody.GameInviteResponseRequest) msg.body();
        assertEquals(GAME_ID, body.gameId());
        assertTrue(body.accept());
    }

    @Test
    void parseGameInviteResponse_decline() throws Exception {
        String xml = request(MSG_ID, "GAME_INVITE_RESPONSE", session(SES_ID),
                "<game-id>%s</game-id><accept>false</accept>".formatted(GAME_ID));
        Message msg = parse(xml);

        MessageBody.GameInviteResponseRequest body = (MessageBody.GameInviteResponseRequest) msg.body();
        assertFalse(body.accept());
    }

    // ── GAME_MOVE ─────────────────────────────────────────────────────────────

    @Test
    void parseGameMoveRequest_rawMoveExtracted() throws Exception {
        String xml = request(MSG_ID, "GAME_MOVE", session(SES_ID),
                "<game-id>%s</game-id><move>1,2</move>".formatted(GAME_ID));
        Message msg = parse(xml);

        assertEquals(ActionType.GAME_MOVE, msg.actionType());
        MessageBody.GameMove body = (MessageBody.GameMove) msg.body();
        assertEquals(GAME_ID, body.gameId());
        assertEquals("1,2",   body.rawMove());
    }

    @Test
    void parseGameMoveRequest_cdataRawMove() throws Exception {
        String xml = request(MSG_ID, "GAME_MOVE", session(SES_ID),
                "<game-id>%s</game-id><move><![CDATA[a&b<c>]]></move>".formatted(GAME_ID));
        Message msg = parse(xml);

        MessageBody.GameMove body = (MessageBody.GameMove) msg.body();
        assertEquals("a&b<c>", body.rawMove());
    }

    // ── UNKNOWN action ────────────────────────────────────────────────────────

    @Test
    void parseUnknownAction_mapsToUnknownBodyAndActionType() throws Exception {
        // UNKNOWN is a valid enum/schema value — parser should handle it gracefully
        String xml = request(MSG_ID, "UNKNOWN", noSession(), "");
        Message msg = parse(xml);

        assertEquals(ActionType.UNKNOWN, msg.actionType());
        assertInstanceOf(MessageBody.Unknown.class, msg.body());
    }

    // ── error cases ──────────────────────────────────────────────────────────

    @Test
    void parseInvalidXML_throwsMessageParseException() {
        String garbage = "not xml at all {{{{";
        assertThrows(MessageParseException.class,
                () -> parser.parseMessage(new ByteArrayInputStream(garbage.getBytes())));
    }

    @Test
    void parseSchemaViolation_throwsMalformedMessageException() {
        // Missing required <action> in header → schema validation failure
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <message type="REQUEST" id="%s" version="1.0">
                    <header>
                        <timestamp>%s</timestamp>
                    </header>
                    <body/>
                </message>
                """.formatted(MSG_ID, TS);
        assertThrows(MalformedMessageException.class,
                () -> parser.parseMessage(new ByteArrayInputStream(xml.getBytes())));
    }

    @Test
    void parseEmptyStream_throwsParseException() {
        assertThrows(MessageParseException.class,
                () -> parser.parseMessage(new ByteArrayInputStream(new byte[0])));
    }
}
