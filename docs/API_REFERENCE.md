# IECD-TP1: API Reference Documentation

## Table of Contents
1. [Protocol Messages](#protocol-messages)
2. [Server API](#server-api)
3. [Handler APIs](#handler-apis)
4. [Storage APIs](#storage-apis)
5. [Message Examples](#message-examples)

---

## Protocol Messages

### Message Envelope

All messages follow this base structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="REQUEST|RESPONSE|PUSH" 
         id="[UUID]" 
         version="1.0">
    <header>
        <action>[ACTION_TYPE]</action>
        <session>[SESSION_UUID]</session>
        <timestamp>[ISO8601_DATETIME]</timestamp>
    </header>
    <body>
        <!-- Action-specific content -->
    </body>
</message>
```

**Attributes**:
- `type`: MESSAGE_TYPE - REQUEST, RESPONSE, or PUSH
- `id`: UUID - Unique message identifier
- `version`: String - Protocol version (always "1.0")

**Header Elements**:
- `action`: ACTION_TYPE - The operation to perform
- `session`: UUID (optional) - Session token for authenticated requests
- `timestamp`: ISO8601 - Server timestamp

---

## Protocol Actions

### REGISTER

Register new user account.

**Request Type**: REQUEST  
**Authentication**: Not required

**Request Body**:
```xml
<body>
    <username>alice</username>
    <password>secret123</password>
    <photo>base64_encoded_image</photo>
    <nationality>PT</nationality>
    <dob>1990-05-15</dob>
</body>
```

**Response (Success)**:
```xml
<body>
    <status>OK</status>
    <session>550e8400-e29b-41d4-a716-446655440000</session>
    <user>
        <id>550e8400-e29b-41d4-a716-446655440001</id>
        <username>alice</username>
        <photo>base64_encoded_image</photo>
        <nationality>PT</nationality>
        <dob>1990-05-15</dob>
        <stats/>
    </user>
</body>
```

**Response (Error)**:
```xml
<body>
    <status>ERROR</status>
    <error code="USERNAME_TAKEN">Username 'alice' is already taken</error>
</body>
```

**Possible Errors**:
- `USERNAME_TAKEN`: Username already in use
- `MALFORMED_REQUEST`: Missing required fields
- `INTERNAL_ERROR`: Server error during registration

---

### LOGIN

Authenticate user and create session.

**Request Type**: REQUEST  
**Authentication**: Not required  
**Session**: Should NOT be included in header

**Request Body**:
```xml
<body>
    <username>alice</username>
    <password>secret123</password>
</body>
```

**Response (Success)**:
```xml
<body>
    <status>OK</status>
    <session>550e8400-e29b-41d4-a716-446655440000</session>
    <user>
        <id>550e8400-e29b-41d4-a716-446655440001</id>
        <username>alice</username>
        <photo>base64_encoded_image</photo>
        <nationality>PT</nationality>
        <dob>1990-05-15</dob>
        <stats>
            <match result="WON" playtime="120.5" 
                   opponent-id="[UUID]" opponent-username="bob"/>
            <match result="LOST" playtime="85.0" 
                   opponent-id="[UUID]" opponent-username="charlie"/>
        </stats>
    </user>
</body>
```

**Response (Error)**:
```xml
<body>
    <status>ERROR</status>
    <error code="AUTH_FAILED">Invalid username or password</error>
</body>
```

**Possible Errors**:
- `AUTH_FAILED`: Invalid credentials
- `USER_NOT_FOUND`: User doesn't exist
- `INVALID_PASSWORD`: Password incorrect

---

### LOGOUT

Invalidate current session.

**Request Type**: REQUEST  
**Authentication**: Required  
**Session**: Must be included

**Request Body**:
```xml
<body/>
```

**Response (Success)**:
```xml
<body>
    <status>OK</status>
</body>
```

**Response (Error)**:
```xml
<body>
    <status>ERROR</status>
    <error code="SESSION_EXPIRED">Session token invalid or expired</error>
</body>
```

---

### UPDATE_PROFILE

Update user profile information.

**Request Type**: REQUEST  
**Authentication**: Required

**Request Body - Update Photo**:
```xml
<body>
    <photo>new_base64_encoded_image</photo>
</body>
```

**Request Body - Update Password**:
```xml
<body>
    <password>new_password</password>
</body>
```

**Response (Success)**:
```xml
<body>
    <status>OK</status>
    <user>
        <id>550e8400-e29b-41d4-a716-446655440001</id>
        <username>alice</username>
        <photo>new_base64_encoded_image</photo>
        <nationality>PT</nationality>
        <dob>1990-05-15</dob>
        <stats/>
    </user>
</body>
```

**Response (Error)**:
```xml
<body>
    <status>ERROR</status>
    <error code="NOT_AUTHENTICATED">Session required</error>
</body>
```

---

### SEARCH_USERS

Search for users by query.

**Request Type**: REQUEST  
**Authentication**: Not required

**Request Body**:
```xml
<body>
    <query>ali</query>
</body>
```

**Response (Success)**:
```xml
<body>
    <status>OK</status>
    <results>
        <user>
            <id>550e8400-e29b-41d4-a716-446655440001</id>
            <username>alice</username>
            <photo>base64_encoded_image</photo>
            <nationality>PT</nationality>
            <dob>1990-05-15</dob>
            <stats>
                <match result="WON" playtime="120.5" 
                       opponent-id="[UUID]" opponent-username="bob"/>
            </stats>
        </user>
        <user>
            <id>550e8400-e29b-41d4-a716-446655440002</id>
            <username>alicia</username>
            <photo>base64_encoded_image</photo>
            <nationality>ES</nationality>
            <dob>1995-08-22</dob>
            <stats/>
        </user>
    </results>
</body>
```

**Response (No Results)**:
```xml
<body>
    <status>OK</status>
    <results/>
</body>
```

---

### GAME_INVITE

Send invitation to play a game.

**Request Type**: REQUEST  
**Authentication**: Required

**Request Body**:
```xml
<body>
    <target-user-id>550e8400-e29b-41d4-a716-446655440002</target-user-id>
    <!-- Game-specific parameters if applicable -->
</body>
```

**Response (Success - to Inviter)**:
```xml
<body>
    <status>OK</status>
    <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
</body>
```

**PUSH Message (to Invitee)**:
```xml
<message type="PUSH" id="[UUID]" version="1.0">
    <header>
        <action>GAME_INVITE</action>
        <session>[INVITEE_SESSION]</session>
        <timestamp>[ISO8601_DATETIME]</timestamp>
    </header>
    <body>
        <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
        <from-user-id>550e8400-e29b-41d4-a716-446655440001</from-user-id>
        <from-username>alice</from-username>
    </body>
</message>
```

**Errors**:
- `NOT_AUTHENTICATED`: Session required
- `USER_NOT_FOUND`: Target user doesn't exist
- `USER_NOT_ONLINE`: Target user not connected
- `ALREADY_IN_GAME`: Target user already in a game

---

### GAME_INVITE_RESPONSE

Accept or decline game invitation.

**Request Type**: REQUEST  
**Authentication**: Required

**Request Body - Accept**:
```xml
<body>
    <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
    <accept>true</accept>
</body>
```

**Request Body - Decline**:
```xml
<body>
    <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
    <accept>false</accept>
</body>
```

**Response (Success)**:
```xml
<body>
    <status>OK</status>
</body>
```

**PUSH Message (if accepted - to Inviter)**:
```xml
<message type="PUSH" id="[UUID]" version="1.0">
    <header>
        <action>GAME_INVITE_RESPONSE</action>
        <session>[INVITER_SESSION]</session>
        <timestamp>[ISO8601_DATETIME]</timestamp>
    </header>
    <body>
        <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
        <accepted>true</accepted>
        <opponent-username>bob</opponent-username>
    </body>
</message>
```

---

### GAME_MOVE

Submit a move in an ongoing game.

**Request Type**: REQUEST  
**Authentication**: Required

**Request Body** (game-specific):
```xml
<body>
    <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
    <move>
        <!-- Game-specific move data -->
    </move>
</body>
```

**Response (Move Accepted)**:
```xml
<body>
    <status>OK</status>
</body>
```

**Response (Move Rejected)**:
```xml
<body>
    <status>ERROR</status>
    <error code="INVALID_MOVE">Move violates game rules</error>
</body>
```

**Errors**:
- `GAME_NOT_FOUND`: Game doesn't exist
- `INVALID_MOVE`: Move is not valid for current game state
- `NOT_AUTHENTICATED`: Session required

---

### GAME_OVER

Sent by server when game concludes.

**Message Type**: PUSH  
**Sent to**: Both players

**PUSH Message**:
```xml
<message type="PUSH" id="[UUID]" version="1.0">
    <header>
        <action>GAME_OVER</action>
        <session>[PLAYER_SESSION]</session>
        <timestamp>[ISO8601_DATETIME]</timestamp>
    </header>
    <body>
        <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
        <winner-id>550e8400-e29b-41d4-a716-446655440001</winner-id>
        <winner-username>alice</winner-username>
    </body>
</message>
```

---

## Server API

### Server Class

```java
package iecd.a51597.server;

public class Server {
    // Singleton Access
    public static Server getInstance()
    
    // Listener Management
    public void startListener()
    public void startListener(int port)
    public void stopListener()
    public boolean isListening()
    
    // Connection Management
    public void addConnection(Connection connection)
    public void removeConnection(Connection connection)
    public List<Connection> getConnections()
    
    // Component Access
    public MessageHandler getMessageHandler()
    public SessionManager getSessionManager()
    public UserStore getUserStore()
    public GameManager getGameManager()
    public Leaderboard getLeaderboard()
    public MessageBuilder getMessageBuilder()
    public CommParser getCommParser()
    
    // Game Management
    public void registerGameFactory(GameFactory factory)
    
    // Server Control
    public void shutdown()
    public int getStartupPort()
}
```

**Example Usage**:
```java
// Get server instance
Server server = Server.getInstance();

// Start listening
server.startListener(5000);

// Register a game
server.registerGameFactory(new MyGameFactory());

// Get components
SessionManager sessions = server.getSessionManager();
UserStore users = server.getUserStore();

// Shutdown
server.shutdown();
```

---

### SessionManager API

```java
package iecd.a51597.server.session;

public class SessionManager {
    // Session Creation & Validation
    UUID createSession(UUID userId)
    Session getSession(UUID sessionToken)
    boolean validateSession(UUID sessionToken)
    void invalidateSession(UUID sessionToken)
    
    // Queries
    UUID getUserId(UUID sessionToken)
}
```

**Example Usage**:
```java
SessionManager sm = server.getSessionManager();

// Create session after login
UUID token = sm.createSession(userId);

// Validate session
boolean valid = sm.validateSession(token);

// Get user ID from token
UUID userId = sm.getUserId(token);

// Logout
sm.invalidateSession(token);
```

---

### UserStore API

```java
package iecd.a51597.server.store;

public class UserStore {
    // User Registration & Authentication
    User registerUser(String username, String password, UserProfile profile)
        throws UsernameAlreadyTakenException
    
    User authenticate(String username, String password)
        throws AuthException
    
    // User Queries
    User getUser(UUID userId)
    User getUserByUsername(String username)
    List<User> searchUsers(String query)
    
    // User Updates
    void updateUser(User user)
    
    // Existence Checks
    boolean userExists(UUID userId)
    boolean usernameExists(String username)
}
```

**Example Usage**:
```java
UserStore store = server.getUserStore();

// Register user
User user = store.registerUser("alice", "pass123", profile);

// Authenticate
User user = store.authenticate("alice", "pass123");

// Get user
User user = store.getUser(userId);

// Search
List<User> results = store.searchUsers("ali");

// Update
store.updateUser(user);
```

---

### GameManager API

```java
package iecd.a51597.server.game;

public class GameManager {
    // Game Factory Registration
    void registerFactory(GameFactory factory)
    GameFactory getFactory(GameType type)
    
    // Game Management
    Game createGame(GameType type, UUID player1Id, UUID player2Id)
    Game getGame(UUID gameId)
    void removeGame(UUID gameId)
    boolean gameExists(UUID gameId)
    
    // Game Queries
    List<Game> getActiveGames()
    int getActiveGameCount()
}
```

**Example Usage**:
```java
GameManager gm = server.getGameManager();

// Register game type
gm.registerFactory(new ChessGameFactory());

// Create game
Game game = gm.createGame(GameType.CHESS, player1Id, player2Id);

// Get game
Game game = gm.getGame(gameId);

// Process move
MoveResult result = game.processMove(playerId, moveData);

// Finish game
gm.removeGame(gameId);
```

---

### Leaderboard API

```java
package iecd.a51597.server.store;

public class Leaderboard {
    // Record Results
    void recordMatch(UUID player1Id, UUID player2Id, UUID winnerId, double playtime)
    
    // Queries
    List<PlayerStats> getTop(int count)
    PlayerStats getStats(UUID userId)
    int getRank(UUID userId)
    
    // Statistics
    double getWinRate(UUID userId)
    int getTotalMatches(UUID userId)
}
```

**Example Usage**:
```java
Leaderboard lb = server.getLeaderboard();

// Record game result
lb.recordMatch(player1Id, player2Id, winnerId, 120.5);

// Get top 10
List<PlayerStats> top10 = lb.getTop(10);

// Get player stats
PlayerStats stats = lb.getStats(userId);

// Get rank
int rank = lb.getRank(userId);
```

---

## Handler APIs

### AuthHandler

```java
package iecd.a51597.server.handlers;

public class AuthHandler extends BaseHandler {
    boolean canHandle(ActionType action)
    Message handle(Message message, Connection connection)
}
```

**Handles**: REGISTER, LOGIN, LOGOUT

---

### ProfileHandler

```java
package iecd.a51597.server.handlers;

public class ProfileHandler extends BaseHandler {
    boolean canHandle(ActionType action)
    Message handle(Message message, Connection connection)
}
```

**Handles**: UPDATE_PROFILE

---

### SearchHandler

```java
package iecd.a51597.server.handlers;

public class SearchHandler extends BaseHandler {
    boolean canHandle(ActionType action)
    Message handle(Message message, Connection connection)
}
```

**Handles**: SEARCH_USERS

---

### GameHandler

```java
package iecd.a51597.server.handlers;

public class GameHandler extends BaseHandler {
    boolean canHandle(ActionType action)
    Message handle(Message message, Connection connection)
}
```

**Handles**: GAME_INVITE, GAME_INVITE_RESPONSE, GAME_MOVE

---

## Storage APIs

### User Entity

```java
package iecd.a51597.server.store;

public class User {
    UUID id                    // Unique identifier
    String username            // Username (unique)
    String passwordHash        // Hashed password
    String photo              // Base64 encoded image
    String nationality        // 2-letter country code
    LocalDate dateOfBirth     // Birth date
    PlayerStats stats         // Game statistics
    
    // Getters/Setters
    UUID getId()
    String getUsername()
    String getPhoto()
    String getNationality()
    LocalDate getDateOfBirth()
    PlayerStats getStats()
    
    void setPhoto(String photo)
    void updatePasswordHash(String newHash)
}
```

---

### PlayerStats Entity

```java
package iecd.a51597.server.store;

public class PlayerStats {
    List<Match> matches        // Game history
    
    // Getters
    List<Match> getMatches()
    int getWins()
    int getLosses()
    double getTotalPlaytime()
    double getWinRate()
    
    // Record Result
    void recordMatch(Match match)
}

public class Match {
    UUID matchId
    UUID opponentId
    String opponentUsername
    MatchResult result            // WON or LOST
    double playtime              // Seconds
    LocalDateTime playedAt
}
```

---

## Message Examples

### Full Exchange: Registration Flow

**1. Client Registers**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="REQUEST" 
         id="a8c49c07-e6c0-4f1a-b5c3-8c4a6d7f8c0a" 
         version="1.0">
    <header>
        <action>REGISTER</action>
        <timestamp>2026-04-20T12:30:45Z</timestamp>
    </header>
    <body>
        <username>alice</username>
        <password>mySecurePassword123!</password>
        <photo>iVBORw0KGgoAAAANSUhEUgAAAAUA...</photo>
        <nationality>PT</nationality>
        <dob>1990-05-15</dob>
    </body>
</message>
```

**2. Server Responds**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="RESPONSE" 
         id="a8c49c07-e6c0-4f1a-b5c3-8c4a6d7f8c0a" 
         version="1.0">
    <header>
        <action>REGISTER</action>
        <session>550e8400-e29b-41d4-a716-446655440000</session>
        <timestamp>2026-04-20T12:30:46Z</timestamp>
    </header>
    <body>
        <status>OK</status>
        <session>550e8400-e29b-41d4-a716-446655440000</session>
        <user>
            <id>550e8400-e29b-41d4-a716-446655440001</id>
            <username>alice</username>
            <photo>iVBORw0KGgoAAAANSUhEUgAAAAUA...</photo>
            <nationality>PT</nationality>
            <dob>1990-05-15</dob>
            <stats/>
        </user>
    </body>
</message>
```

### Full Exchange: Game Invitation

**1. Player A Sends Invitation**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="REQUEST" 
         id="c1a49c07-e6c0-4f1a-b5c3-8c4a6d7f8c0b" 
         version="1.0">
    <header>
        <action>GAME_INVITE</action>
        <session>550e8400-e29b-41d4-a716-446655440000</session>
        <timestamp>2026-04-20T12:31:00Z</timestamp>
    </header>
    <body>
        <target-user-id>550e8400-e29b-41d4-a716-446655440002</target-user-id>
    </body>
</message>
```

**2. Server Responds to Player A**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="RESPONSE" 
         id="c1a49c07-e6c0-4f1a-b5c3-8c4a6d7f8c0b" 
         version="1.0">
    <header>
        <action>GAME_INVITE</action>
        <session>550e8400-e29b-41d4-a716-446655440000</session>
        <timestamp>2026-04-20T12:31:01Z</timestamp>
    </header>
    <body>
        <status>OK</status>
        <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
    </body>
</message>
```

**3. Server Pushes Notification to Player B**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="PUSH" 
         id="d2b49c07-e6c0-4f1a-b5c3-8c4a6d7f8c0c" 
         version="1.0">
    <header>
        <action>GAME_INVITE</action>
        <session>550e8400-e29b-41d4-a716-446655440010</session>
        <timestamp>2026-04-20T12:31:02Z</timestamp>
    </header>
    <body>
        <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
        <from-user-id>550e8400-e29b-41d4-a716-446655440001</from-user-id>
        <from-username>alice</from-username>
    </body>
</message>
```

**4. Player B Accepts**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="REQUEST" 
         id="e3c49c07-e6c0-4f1a-b5c3-8c4a6d7f8c0d" 
         version="1.0">
    <header>
        <action>GAME_INVITE_RESPONSE</action>
        <session>550e8400-e29b-41d4-a716-446655440010</session>
        <timestamp>2026-04-20T12:31:05Z</timestamp>
    </header>
    <body>
        <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
        <accept>true</accept>
    </body>
</message>
```

**5. Server Responds to Player B**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="RESPONSE" 
         id="e3c49c07-e6c0-4f1a-b5c3-8c4a6d7f8c0d" 
         version="1.0">
    <header>
        <action>GAME_INVITE_RESPONSE</action>
        <session>550e8400-e29b-41d4-a716-446655440010</session>
        <timestamp>2026-04-20T12:31:06Z</timestamp>
    </header>
    <body>
        <status>OK</status>
    </body>
</message>
```

**6. Server Pushes Acceptance Notification to Player A**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="PUSH" 
         id="f4d49c07-e6c0-4f1a-b5c3-8c4a6d7f8c0e" 
         version="1.0">
    <header>
        <action>GAME_INVITE_RESPONSE</action>
        <session>550e8400-e29b-41d4-a716-446655440000</session>
        <timestamp>2026-04-20T12:31:07Z</timestamp>
    </header>
    <body>
        <game-id>550e8400-e29b-41d4-a716-446655440100</game-id>
        <accepted>true</accepted>
        <opponent-username>bob</opponent-username>
    </body>
</message>
```

---

## Error Response Format

All errors follow this structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="RESPONSE" 
         id="[UUID]" 
         version="1.0">
    <header>
        <action>[ORIGINAL_ACTION]</action>
        <session>[SESSION_IF_AVAILABLE]</session>
        <timestamp>[ISO8601_DATETIME]</timestamp>
    </header>
    <body>
        <status>ERROR</status>
        <error code="ERROR_CODE">Human readable error message</error>
    </body>
</message>
```

**Standard Error Codes**:
- `AUTH_FAILED`: Authentication failed
- `SESSION_EXPIRED`: Session token invalid or expired
- `NOT_AUTHENTICATED`: Authentication required but not provided
- `USERNAME_TAKEN`: Username already exists
- `USER_NOT_FOUND`: User doesn't exist
- `GAME_NOT_FOUND`: Game doesn't exist
- `USER_NOT_ONLINE`: User not currently connected
- `ALREADY_IN_GAME`: User already playing a game
- `INVALID_MOVE`: Move violates game rules
- `INVALID_PASSWORD`: Password incorrect
- `UNKNOWN_ACTION`: Action not recognized
- `INTERNAL_ERROR`: Server-side error
- `MALFORMED_REQUEST`: XML parsing failed
- `UNEXPECTED_MESSAGE_TYPE`: Wrong message type for action
- `UNEXPECTED_MESSAGE_ACTION`: Wrong action for message type
- `OUTDATED_PROTOCOL`: Protocol version mismatch

---

**End of API Reference**

