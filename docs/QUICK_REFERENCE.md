# IECD-TP1: Quick Reference Guide

## File Organization at a Glance

```
IECD-TP1/
├── CODEBASE_DOCUMENTATION.md       ← Start here for overview
├── API_REFERENCE.md                 ← Protocol & APIs
├── DEVELOPER_GUIDE.md               ← How to extend/develop
├── QUICK_REFERENCE.md              ← This file
├── pom.xml                          ← Maven build config
├── config.xml                       ← Server configuration
├── data/
│   └── users.xml                   ← User data (persisted)
├── src/main/java/iecd/a51597/
│   ├── server/
│   │   ├── Server.java             ← Main entry point
│   │   ├── handlers/               ← Message routing
│   │   ├── game/                   ← Game system
│   │   ├── network/                ← TCP connections
│   │   ├── session/                ← Session management
│   │   ├── store/                  ← Data storage
│   │   └── persistence/            ← File persistence
│   ├── common/protocol/            ← XML protocol
│   └── client/                     ← Client stub
└── logs/                           ← Runtime logs
```

---

## Essential Commands

### Building
```bash
mvn clean compile          # Compile
mvn package               # Build JAR
mvn clean install         # Install to local repo
```

### Running
```bash
# Direct execution
java -cp target/classes:. iecd.a51597.server.Server

# With custom port
java -cp target/classes:. iecd.a51597.server.Server 6000

# Using Maven
mvn exec:java -Dexec.mainClass="iecd.a51597.server.Server"
```

### Cleaning
```bash
mvn clean                 # Remove target directory
```

---

## Key Classes & Their Roles

| Class | Location | Purpose |
|-------|----------|---------|
| Server | server/ | Singleton main server, coordinates all subsystems |
| Connection | server/network/ | Handles individual client connections |
| ListenerThread | server/network/ | Accepts incoming TCP connections |
| MessageHandler | server/handlers/ | Routes messages to appropriate handlers |
| AuthHandler | server/handlers/ | Handles REGISTER, LOGIN, LOGOUT |
| GameHandler | server/handlers/ | Handles game-related actions |
| SessionManager | server/session/ | Manages user sessions and tokens |
| UserStore | server/store/ | In-memory user data storage |
| User | server/store/ | User entity with stats |
| GameManager | server/game/ | Manages active games |
| Message | common/protocol/ | XML protocol message record |
| XMLParser | common/protocol/ | Parses XML to Message objects |
| XMLMessageBuilder | common/protocol/ | Creates Message objects to XML |

---

## Component Interactions

```
Client (TCP) ↔ Server.ListenerThread
                   ↓
           Connection (per client)
                   ↓
           MessageHandler.route()
                   ↓
    ┌──────┬──────┬──────┐
    ↓      ↓      ↓      ↓
  Auth  Profile Search Game
    ↓      ↓      ↓      ↓
  UserStore ← SessionManager
    ↓             ↓
  User         Session
    ↓             
  Leaderboard
    
Persistent ← UserStore
Storage
(XML)
```

---

## Quick Protocol Reference

### Message Structure
```xml
<message type="REQUEST|RESPONSE|PUSH" id="UUID" version="1.0">
    <header>
        <action>ACTION_NAME</action>
        <session>SESSION_UUID</session>
        <timestamp>ISO8601</timestamp>
    </header>
    <body>
        <!-- Action-specific fields -->
    </body>
</message>
```

### Common Actions
- **REGISTER**: Create account
- **LOGIN**: Authenticate (creates session)
- **SEARCH_USERS**: Find users
- **GAME_INVITE**: Invite to play
- **GAME_MOVE**: Submit move
- **GAME_OVER**: Game ended (PUSH)

### Response Format
```xml
<body>
    <status>OK|ERROR</status>
    <error code="ERROR_CODE">Message</error>
</body>
```

---

## Configuration Essentials

### config.xml
```xml
<server-configuration>
    <network>
        <default-port>5000</default-port>
    </network>
    <session>
        <timeout-minutes>1440</timeout-minutes>
    </session>
</server-configuration>
```

### Runtime Arguments
```bash
java ... Server 6000          # Custom port
java -Dlog.level=DEBUG ...    # Debug logging
```

---

## Data Files

| File | Format | Purpose |
|------|--------|---------|
| data/users.xml | XML | User data persistence |
| logs/main.log | Text | Main server log |
| logs/clients.log | Text | Client connection log |
| logs/protocol.log | Text | Protocol message log |

---

## Testing Scenarios

### 1. Registration
```
1. Send REGISTER request with username/password
2. Server creates User, assigns UUID
3. Server creates Session token
4. Server persists to users.xml
5. Response includes session token and user data
```

### 2. Login
```
1. Send LOGIN request with credentials
2. Server validates password hash
3. Server creates new Session
4. Response includes session token
5. User can now send authenticated requests
```

### 3. Game Flow
```
1. Player A: GAME_INVITE to Player B
2. Server creates Game instance, notifies B (PUSH)
3. Player B: GAME_INVITE_RESPONSE accept
4. Server notifies A (PUSH)
5. Players: GAME_MOVE alternately
6. When game ends: Server sends GAME_OVER (PUSH to both)
7. Server records match in leaderboard
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port already in use | Use different port: `Server 6000` |
| Users lost after restart | Check data/users.xml exists & has write permissions |
| Session timeout | Increase timeout-minutes in config.xml |
| XML parsing errors | Validate message against protocol.xsd |
| Connection refused | Check firewall, port not blocked |
| Memory issues | Increase JVM: `java -Xmx512m ...` |

---

## Directory Structure in Code

### Server Package
```
server/
├── Server.java                 - Main class
├── cli/
│   └── CLIHandler.java        - Command-line interface
├── config/
│   └── ServerConfiguration.java - Config loader
├── game/
│   ├── Game.java              - Game interface
│   ├── GameFactory.java       - Factory interface
│   ├── GameManager.java       - Game lifecycle
│   ├── Move.java              - Move representation
│   ├── MoveCodec.java         - Move encoding
│   └── MoveResult.java        - Move outcome
├── handlers/
│   ├── BaseHandler.java       - Base class
│   ├── AuthHandler.java       - Auth actions
│   ├── ProfileHandler.java    - Profile actions
│   ├── SearchHandler.java     - Search actions
│   ├── GameHandler.java       - Game actions
│   └── MessageHandler.java    - Router
├── network/
│   ├── Connection.java        - Per-client connection
│   └── ListenerThread.java    - Listener thread
├── persistence/
│   └── PersistenceManager.java - XML serialization
├── session/
│   ├── Session.java           - Session entity
│   └── SessionManager.java    - Session lifecycle
└── store/
    ├── User.java              - User entity
    ├── UserStore.java         - User storage
    ├── PlayerStats.java       - Game stats
    ├── Leaderboard.java       - Rankings
    └── exceptions/            - Custom exceptions
```

### Protocol Package
```
common/protocol/
├── Message.java               - Message record
├── MessageBody.java           - Message payload
├── ProtocolConstants.java     - Constants
├── builders/
│   ├── MessageBuilder.java    - Builder interface
│   └── XMLMessageBuilder.java - XML implementation
├── parsers/
│   ├── CommParser.java        - Parser interface
│   └── XMLParser.java         - XML implementation
├── types/
│   ├── ActionType.java        - Action enum
│   ├── MessageType.java       - Message type enum
│   └── ErrorCodeType.java     - Error codes
└── exceptions/
    ├── CommException.java     - Base exception
    ├── MalformedMessageException.java
    └── MessageParseException.java
```

---

## Common Code Patterns

### Getting Server Instance
```java
Server server = Server.getInstance();
```

### Creating Response Message
```java
Message response = messageBuilder.createSuccessResponse(
    messageId,
    actionType,
    "Operation successful"
);
```

### Error Response
```java
Message error = messageBuilder.createErrorResponse(
    messageId,
    ErrorCodeType.AUTH_FAILED,
    "Invalid credentials"
);
```

### Validating Session
```java
if (!sessionManager.validateSession(message.sessionToken())) {
    return messageBuilder.createErrorResponse(
        messageId,
        ErrorCodeType.SESSION_EXPIRED,
        "Session expired"
    );
}
```

### Getting Current User
```java
UUID userId = sessionManager.getUserId(message.sessionToken());
User user = userStore.getUser(userId);
```

### Creating Game
```java
Game game = gameManager.createGame(
    GameType.CHESS,
    player1Id,
    player2Id
);
```

### Recording Game Result
```java
leaderboard.recordMatch(player1Id, player2Id, winnerId, playtime);
```

---

## Important Concepts

### Singleton Pattern (Server)
- Only one Server instance exists application-wide
- Initialized on first `getInstance()` call
- Thread-safe double-checked locking

### Session Management
- User logs in → Session created with UUID token
- Each authenticated request includes session token
- Session expires after configured timeout
- Logout invalidates session token

### Thread-Safety
- `Connection` runs in dedicated thread per client
- `UserStore` uses synchronization for multi-threaded access
- `SessionManager` uses thread-safe maps
- `GameManager` manages concurrent games

### Persistence
- On startup: `PersistenceManager.load()` reads users.xml
- During runtime: All changes in-memory only
- On shutdown: `PersistenceManager.save()` writes users.xml
- Graceful shutdown ensures no data loss

### Message Routing
1. `Connection` receives XML
2. `XMLParser` converts to `Message` object
3. `MessageHandler` examines `actionType`
4. `MessageHandler` delegates to appropriate handler
5. Handler processes and returns response `Message`
6. Response serialized to XML and sent back

---

## Performance Tips

1. **Reuse Parser/Builder**: Don't create new instances per message
2. **Connection Pooling**: Consider for database access
3. **Caching**: Cache frequently accessed users/data
4. **Batch Operations**: Process multiple updates together
5. **Async I/O**: Use separate threads for network I/O

---

## Security Considerations

1. **Password Hashing**: Always hash before storage
2. **Session Tokens**: Use UUID, invalidate on logout
3. **SQL Injection**: Use parameterized queries (future DB)
4. **Input Validation**: Validate all incoming messages
5. **Error Messages**: Don't expose internal details
6. **HTTPS**: Use in production (not HTTP)
7. **Authentication**: Always validate session tokens

---

## Useful Maven Commands

```bash
# View dependencies
mvn dependency:tree

# Check for updates
mvn versions:display-updates

# Run specific test
mvn test -Dtest=UserStoreTest

# Generate Javadoc
mvn javadoc:javadoc

# Skip tests during build
mvn compile -DskipTests

# Run with different configuration
mvn clean compile -Denv=production
```

---

## Next Steps for New Developers

1. **Read**: CODEBASE_DOCUMENTATION.md (overview)
2. **Explore**: Start with Server.java, understand initialization
3. **Trace**: Follow a REGISTER request through the code
4. **Build**: Run `mvn clean compile` successfully
5. **Debug**: Add breakpoints, step through code
6. **Modify**: Make a small test change (add log line)
7. **Extend**: Follow DEVELOPER_GUIDE.md for new features

---

## Quick Links

- Protocol Schema: `src/main/resources/protocol.xsd`
- Configuration: `config.xml`
- User Data: `data/users.xml`
- Logs: `logs/` directory
- Docs: `CODEBASE_DOCUMENTATION.md`
- API Reference: `API_REFERENCE.md`
- Developer Guide: `DEVELOPER_GUIDE.md`

---

**For detailed information, see the main documentation files.**

Last Updated: 2026-04-20

