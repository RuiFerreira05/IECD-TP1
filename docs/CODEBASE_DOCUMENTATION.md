# IECD-TP1: Complete Codebase Documentation

**Project Name:** IECD-TP1 - Internet & Communication for Distributed Systems - Practical Work 1  
**Version:** 1.0-SNAPSHOT  
**Java Version:** Java 25  
**Build System:** Maven  
**Date:** April 20, 2026

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [Core Components](#core-components)
5. [Package Reference](#package-reference)
6. [Key Classes](#key-classes)
7. [Communication Protocol](#communication-protocol)
8. [Data Persistence](#data-persistence)
9. [Game Management](#game-management)
10. [Session Management](#session-management)
11. [Error Handling](#error-handling)
12. [Configuration](#configuration)
13. [Build & Deployment](#build--deployment)
14. [Getting Started](#getting-started)

---

## Project Overview

### Purpose
IECD-TP1 is a distributed systems project implementing a real-time game server with XML-based communication protocol. The system enables users to:
- Register and authenticate
- Manage user profiles
- Search for other players
- Engage in multiplayer games
- Track game statistics and leaderboards

### Key Features
- **User Management**: Registration, login, profile updates
- **Real-time Communication**: XML-based protocol for client-server interaction
- **Game Framework**: Extensible game management system
- **Persistence**: File-based user data persistence
- **Session Management**: Secure session-based authentication
- **Leaderboard**: Track player statistics and rankings

### Technology Stack
- **Language**: Java 25
- **Logging**: Apache Log4j 2.25.3
- **Data Format**: XML
- **Architecture**: Server-Client
- **Concurrency**: Thread-based handling

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                            │
│  (Client.java - Stub for client implementation)                 │
└─────────────────────────────────────────────────────────────────┘
                              ↕
                    ┌─────────────────────┐
                    │ Communication Layer │
                    │ (XML Protocol)      │
                    └─────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                       SERVER LAYER                              │
│ ┌────────────────────────────────────────────────────────────┐ │
│ │                    SERVER (Singleton)                     │ │
│ │  - Initializes all subsystems                            │ │
│ │  - Manages server lifecycle                              │ │
│ │  - Coordinates components                                │ │
│ └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────┐           │
│  │   NETWORK    │  │   PROTOCOL  │  │  HANDLERS    │           │
│  │              │  │              │  │              │           │
│  │• Listener    │  │• Message    │  │• Auth        │           │
│  │• Connection  │  │• Builder    │  │• Profile     │           │
│  └──────────────┘  │• Parser     │  │• Search      │           │
│                    │• Types      │  │• Game        │           │
│  ┌──────────────┐  └─────────────┘  └──────────────┘           │
│  │  GAME ENGINE │                                              │
│  │              │  ┌──────────────┐  ┌──────────────┐           │
│  │• Game Manager│  │   SESSION    │  │     DATA     │           │
│  │• Game Factory│  │              │  │              │           │
│  │• Move Codec  │  │• Session Mgr │  │• User Store  │           │
│  └──────────────┘  │• Session     │  │• Leaderboard │           │
│                    │              │  │• Persistence │           │
│                    └──────────────┘  └──────────────┘           │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              STORAGE LAYER                              │  │
│  │  - users.xml (User persistence)                         │  │
│  │  - Config files (Server configuration)                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Design Patterns

1. **Singleton Pattern**: Server class
2. **Factory Pattern**: GameFactory for game creation
3. **Strategy Pattern**: MessageBuilder and CommParser
4. **Handler Pattern**: Specific handlers for different message types
5. **Observer Pattern**: Session and connection management

---

## Project Structure

```
IECD-TP1/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── iecd/a51597/
│   │   │       ├── client/
│   │   │       │   └── Client.java
│   │   │       ├── common/
│   │   │       │   └── protocol/
│   │   │       │       ├── Message.java
│   │   │       │       ├── MessageBody.java
│   │   │       │       ├── ProtocolConstants.java
│   │   │       │       ├── builders/
│   │   │       │       │   ├── MessageBuilder.java (interface)
│   │   │       │       │   └── XMLMessageBuilder.java
│   │   │       │       ├── exceptions/
│   │   │       │       │   ├── CommException.java
│   │   │       │       │   ├── MalformedMessageException.java
│   │   │       │       │   └── MessageParseException.java
│   │   │       │       ├── parsers/
│   │   │       │       │   ├── CommParser.java (interface)
│   │   │       │       │   └── XMLParser.java
│   │   │       │       └── types/
│   │   │       │           ├── ActionType.java
│   │   │       │           ├── ErrorCodeType.java
│   │   │       │           └── MessageType.java
│   │   │       └── server/
│   │   │           ├── Server.java (main server class)
│   │   │           ├── cli/
│   │   │           │   └── CLIHandler.java
│   │   │           ├── config/
│   │   │           │   └── ServerConfiguration.java
│   │   │           ├── game/
│   │   │           │   ├── Game.java (interface)
│   │   │           │   ├── GameFactory.java (interface)
│   │   │           │   ├── GameManager.java
│   │   │           │   ├── Move.java
│   │   │           │   ├── MoveCodec.java
│   │   │           │   └── MoveResult.java
│   │   │           ├── handlers/
│   │   │           │   ├── BaseHandler.java
│   │   │           │   ├── AuthHandler.java
│   │   │           │   ├── GameHandler.java
│   │   │           │   ├── MessageHandler.java
│   │   │           │   ├── ProfileHandler.java
│   │   │           │   └── SearchHandler.java
│   │   │           ├── network/
│   │   │           │   ├── Connection.java
│   │   │           │   └── ListenerThread.java
│   │   │           ├── persistence/
│   │   │           │   └── PersistenceManager.java
│   │   │           ├── session/
│   │   │           │   ├── Session.java
│   │   │           │   └── SessionManager.java
│   │   │           └── store/
│   │   │               ├── Leaderboard.java
│   │   │               ├── PlayerStats.java
│   │   │               ├── User.java
│   │   │               ├── UserStore.java
│   │   │               └── exceptions/
│   │   │                   ├── StoreException.java
│   │   │                   └── UsernameAlreadyTakenException.java
│   │   └── resources/
│   │       ├── config.xsd
│   │       ├── log4j2.xml
│   │       ├── protocol.xsd
│   │       └── users.xsd
│   └── test/
│       └── java/ (test directory)
├── data/
│   └── users.xml (persisted user data)
├── docs/
│   ├── protocol-reference.txt
│   └── TrabalhoPratico-PT.pdf
├── logs/
│   ├── clients.log
│   ├── main.log
│   └── protocol.log
├── config.xml (server configuration)
├── pom.xml (Maven configuration)
└── target/ (compiled artifacts)
```

---

## Core Components

### 1. Server Component

**Class**: `Server.java`  
**Pattern**: Singleton  
**Responsibility**: Main entry point, initializes and orchestrates all subsystems

#### Key Methods:
- `getInstance()`: Get singleton instance
- `startListener(port)`: Start network listener
- `stopListener()`: Stop listening for connections
- `shutdown()`: Graceful shutdown
- `registerGameFactory(factory)`: Register game implementations

#### Key Fields:
```java
- port: int                           // Server listening port
- sessionManager: SessionManager      // Session token management
- messageHandler: MessageHandler      // Route messages to handlers
- gameManager: GameManager            // Manage active games
- userStore: UserStore               // User data storage
- leaderboard: Leaderboard           // Player rankings
- persistenceManager: PersistenceManager // Save/load user data
- connections: List<Connection>      // Active connections
```

#### Initialization Sequence:
1. Load server configuration
2. Initialize message parser and builder
3. Initialize user store and persistence
4. Initialize all handlers (Auth, Profile, Search, Game)
5. Create CLI handler
6. Start listener thread
7. Start CLI thread

---

### 2. Network Components

#### ListenerThread.java
**Responsibility**: Accept incoming connections and manage network connections

**Methods**:
- `run()`: Listen for incoming connections
- `stopListener()`: Signal thread to stop
- `isRunning()`: Check if listener is active

**Connection Handling**:
- Accepts TCP connections on configured port
- Creates Connection object for each client
- Manages connection lifecycle

#### Connection.java
**Responsibility**: Handle individual client connections

**Methods**:
- `run()`: Handle incoming messages from client
- `sendMessage(message)`: Send response to client
- `closeConnection()`: Close the connection
- `isConnected()`: Check connection status

**Communication Flow**:
1. Receive XML message from client
2. Parse message using XMLParser
3. Route to appropriate handler
4. Send response back to client

---

### 3. Protocol Components

#### Message.java
**Type**: Java Record  
**Structure**:
```java
record Message(
    UUID messageId,              // Unique message identifier
    MessageType messageType,     // REQUEST, RESPONSE, PUSH
    String version,              // Protocol version
    ActionType actionType,       // REGISTER, LOGIN, GAME_MOVE, etc.
    UUID sessionToken,           // Session authentication token
    MessageBody body             // Message payload
)
```

#### MessageBody.java
**Responsibility**: Container for message payloads

**Nested Classes**:
- `Login`: Username/password
- `Register`: Registration data
- `GameInvite`: Game invitation
- `GameMove`: Game move data
- `GameOver`: Game end data
- And more...

#### Protocol Types

**ActionType**: Enum of all possible actions
```
UNKNOWN, REGISTER, LOGIN, LOGOUT, UPDATE_PROFILE,
SEARCH_USERS, GAME_INVITE, GAME_INVITE_RESPONSE,
GAME_MOVE, GAME_OVER
```

**MessageType**: Enum of message types
```
REQUEST    - Client request to server
RESPONSE   - Server response to client
PUSH       - Server-initiated message to client
```

**ErrorCodeType**: Comprehensive error codes
```
AUTH_FAILED, USERNAME_TAKEN, SESSION_EXPIRED,
NOT_AUTHENTICATED, USER_NOT_FOUND, GAME_NOT_FOUND,
USER_NOT_ONLINE, ALREADY_IN_GAME, INVALID_MOVE,
INVALID_PASSWORD, UNKNOWN_ACTION, INTERNAL_ERROR,
MALFORMED_REQUEST, UNEXPECTED_MESSAGE_TYPE,
UNEXPECTED_MESSAGE_ACTION, OUTDATED_PROTOCOL
```

---

### 4. Handler Components

#### MessageHandler.java
**Responsibility**: Route incoming messages to appropriate handler

**Method**:
- `handle(message, connection)`: Process message and generate response

**Routing Logic**:
```
Message.actionType
├── REGISTER/LOGIN/LOGOUT/UPDATE_PROFILE
│   → AuthHandler or ProfileHandler
├── SEARCH_USERS
│   → SearchHandler
├── GAME_* actions
│   → GameHandler
└── Unknown → Error response
```

#### BaseHandler.java
**Responsibility**: Base class for all handlers

**Methods**:
- `canHandle(actionType)`: Check if handler processes action
- `handle(message, connection)`: Process message

#### AuthHandler.java
**Responsibility**: Handle authentication-related actions

**Handles**:
- `REGISTER`: Create new user account
- `LOGIN`: Authenticate user and create session
- `LOGOUT`: Invalidate session

**Key Operations**:
1. Validate input (username, password)
2. Check username availability
3. Hash password securely
4. Create/validate session tokens
5. Return user data on success

#### ProfileHandler.java
**Responsibility**: Handle user profile updates

**Handles**:
- `UPDATE_PROFILE`: Modify user information

**Operations**:
- Update photo, nationality, DOB
- Update password (with old password verification)

#### SearchHandler.java
**Responsibility**: Handle user search operations

**Handles**:
- `SEARCH_USERS`: Find users by query

**Operations**:
- Search users by username
- Return user profiles with stats

#### GameHandler.java
**Responsibility**: Handle game-related operations

**Handles**:
- `GAME_INVITE`: Send game invitation
- `GAME_INVITE_RESPONSE`: Accept/decline invitation
- `GAME_MOVE`: Submit game move
- `GAME_OVER`: Finish game and record stats

**Key Operations**:
1. Create game instances
2. Manage game state
3. Process moves
4. Update leaderboard

---

### 5. Game Management Components

#### GameManager.java
**Responsibility**: Manage active games

**Methods**:
- `registerFactory(GameFactory)`: Register game type
- `createGame(type, player1, player2)`: Create new game
- `getGame(gameId)`: Retrieve game
- `removeGame(gameId)`: Finish game

**Data Structure**:
```java
Map<GameType, GameFactory> gameFactories    // Available game types
Map<UUID, Game> activeGames                 // Currently running games
```

#### Game.java (Interface)
**Responsibility**: Abstract game behavior

**Methods**:
```java
UUID getGameId()
GameType getType()
MoveResult processMove(playerId, moveData)
boolean isFinished()
UUID getWinnerId()
```

#### GameFactory.java (Interface)
**Responsibility**: Create game instances

**Methods**:
```java
Game createGame(player1Id, player2Id)
GameType getGameType()
```

#### MoveCodec.java
**Responsibility**: Encode/decode game moves

**Methods**:
- `encode(Move)`: Convert Move to XML/transmission format
- `decode(String)`: Parse move data

#### MoveResult.java
**Responsibility**: Encapsulate move outcomes

**Subtypes**:
- `Accepted`: Move was valid, game continues
- `Rejected`: Move invalid, game state unchanged
- `GameOver`: Move ended game

---

### 6. Session Management

#### SessionManager.java
**Responsibility**: Manage user sessions

**Methods**:
- `createSession(userId)`: Create new session token
- `getSession(token)`: Retrieve session
- `validateSession(token)`: Check if token is valid
- `invalidateSession(token)`: End session (logout)

**Features**:
- Session expiration
- Thread-safe session storage
- Session lookup by token

#### Session.java
**Responsibility**: Represent single user session

**Fields**:
```java
UUID sessionToken         // Unique session identifier
UUID userId              // User ID for this session
LocalDateTime createdAt  // Session creation time
LocalDateTime expiresAt  // Session expiration time
```

---

### 7. Data Storage Components

#### UserStore.java
**Responsibility**: In-memory user data storage

**Methods**:
- `registerUser(username, password, profile)`: Create new user
- `getUser(userId)`: Retrieve user by ID
- `getUserByUsername(username)`: Find user by username
- `updateUser(user)`: Update user data
- `authenticate(username, password)`: Verify credentials

**Features**:
- Thread-safe with synchronization
- Password hashing
- Username uniqueness enforcement

#### User.java
**Responsibility**: Represent user entity

**Fields**:
```java
UUID id                 // Unique user identifier
String username         // Username (unique)
String passwordHash     // Hashed password
String photo           // User photo (Base64 or URL)
String nationality     // 2-letter country code
LocalDate dateOfBirth // User's date of birth
PlayerStats stats      // Game statistics
```

#### PlayerStats.java
**Responsibility**: Track player game statistics

**Fields**:
```java
List<Match> matches    // Game history
int wins               // Total wins
int losses             // Total losses
double totalPlaytime   // Total time played
```

#### Leaderboard.java
**Responsibility**: Maintain ranked player list

**Methods**:
- `recordMatch(result)`: Add match to leaderboard
- `getTop(count)`: Get top N players
- `getRank(userId)`: Get player's rank
- `getStats(userId)`: Get player statistics

**Ranking Criteria**:
- Win-loss ratio
- Total matches played
- Total playtime

---

### 8. Persistence Components

#### PersistenceManager.java
**Responsibility**: Save/load user data from XML

**Methods**:
- `load()`: Load users.xml into memory
- `save()`: Write UserStore to users.xml
- `saveUser(user)`: Save individual user

**Format**: XML according to users.xsd schema

**Flow**:
1. Server startup: load() reads users.xml
2. During runtime: users stored in UserStore (in-memory)
3. Server shutdown: save() writes to users.xml
4. Profile updates: saveUser() for incremental updates

---

### 9. Configuration Components

#### ServerConfiguration.java
**Responsibility**: Load and manage server configuration

**Configuration Items**:
- Default port (typically 5000)
- Log levels
- Session timeout duration
- Database paths
- Game settings

**Configuration Source**: config.xml

---

### 10. Command-Line Interface

#### CLIHandler.java
**Responsibility**: Handle interactive command-line interface

**Available Commands**:
```
start [port]            - Start server listener
stop                    - Stop server listener
status                  - Show server status
users                   - List connected users
kick <userId>          - Disconnect user
leaderboard            - Show top players
reload                 - Reload configuration
shutdown               - Gracefully shut down server
help                   - Show available commands
```

---

## Communication Protocol

### Protocol Overview

**Format**: XML  
**Schema**: Defined in `protocol.xsd`  
**Encoding**: UTF-8  
**Transport**: TCP/IP

### Message Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="REQUEST|RESPONSE|PUSH" 
         id="uuid" 
         version="1.0">
    <header>
        <action>ACTION_NAME</action>
        <session>session-uuid</session>
        <timestamp>2026-04-20T12:30:45Z</timestamp>
    </header>
    <body>
        <!-- Action-specific fields -->
    </body>
</message>
```

### Request-Response Flows

#### 1. Registration Flow
```
Client Request (REGISTER):
├─ username: string (required)
├─ password: string (required)
└─ (optional profile data)

Server Response:
├─ status: OK | ERROR
├─ session: uuid (on success)
└─ user: {...} (on success)
```

#### 2. Login Flow
```
Client Request (LOGIN):
├─ username: string (required)
├─ password: string (required)
└─ session: (none - not included)

Server Response:
├─ status: OK | ERROR
├─ session: uuid (on success)
├─ user: {...} (on success)
└─ error: error details (on failure)
```

#### 3. Game Invitation Flow
```
Client Request (GAME_INVITE):
├─ session: uuid
├─ target-user-id: uuid
└─ (other game parameters)

Server Response (to inviter):
├─ status: OK | ERROR
├─ game-id: uuid

Server PUSH (to invitee):
├─ message type: PUSH
├─ action: GAME_INVITE
├─ from-user-id: uuid
├─ from-username: string
└─ game-id: uuid
```

#### 4. Game Move Flow
```
Client Request (GAME_MOVE):
├─ session: uuid
├─ game-id: uuid
├─ move: <move-specific-data>
└─ (move parameters based on game type)

Server Response:
├─ status: OK | ERROR
└─ (move accepted/rejected)

If game ends:
Server PUSH (to both players):
├─ message type: PUSH
├─ action: GAME_OVER
├─ game-id: uuid
├─ winner-id: uuid
└─ winner-username: string
```

### Error Handling

All error responses follow this structure:
```xml
<error code="ERROR_CODE">Human readable message</error>
```

**Common Error Codes**:
- `AUTH_FAILED`: Invalid credentials
- `SESSION_EXPIRED`: Session token invalid/expired
- `USERNAME_TAKEN`: Registration username already in use
- `NOT_AUTHENTICATED`: Required session missing
- `USER_NOT_FOUND`: Target user doesn't exist
- `GAME_NOT_FOUND`: Game instance not found
- `INVALID_MOVE`: Move violates game rules
- `MALFORMED_REQUEST`: XML parsing error

---

## Data Persistence

### XML Schema Files

#### users.xsd
Defines structure for user data file:
```xml
<user>
    <id>uuid</id>
    <username>string</username>
    <photo>base64</photo>
    <nationality>string</nationality>
    <dob>date</dob>
    <stats>
        <match result="WON|LOST" 
               playtime="double"
               opponent-id="uuid"
               opponent-username="string"/>
    </stats>
</user>
```

#### config.xsd
Defines server configuration structure

#### protocol.xsd
Defines all valid message structures (attached to project)

### Persistence Strategy

**Timing**:
- **On Startup**: Load users.xml into UserStore
- **During Runtime**: Updates in-memory only
- **On Shutdown**: Write UserStore back to users.xml
- **On Profile Changes**: Optionally save incrementally

**Thread Safety**:
- UserStore synchronized for concurrent access
- PersistenceManager thread-safe for save operations

**Data Loss Prevention**:
- Graceful shutdown ensures data saved
- Session data not persisted (ephemeral)
- Game history stored in user stats

---

## Game Management

### Game System Architecture

**Extensibility**: Games are registered via `GameFactory` interface

**Game Registration Flow**:
```java
Server server = Server.getInstance();
GameFactory myGameFactory = new MyGameFactory();
server.registerGameFactory(myGameFactory);
```

**Custom Game Implementation**:
```java
public class MyGame implements Game {
    @Override
    public UUID getGameId() { return gameId; }
    
    @Override
    public MoveResult processMove(UUID playerId, String moveData) {
        // Validate move
        // Update game state
        // Check win condition
        // Return result
    }
}
```

### Move Processing

**Move Format**: Game-specific XML payload
```xml
<move>
    <!-- Game-specific content -->
</move>
```

**Move Codec**: Encodes/decodes moves between transmission format and game objects

**Move Result Types**:
- `Accepted`: Move valid, game continues
- `Rejected`: Move invalid, reason provided
- `GameOver`: Move ended game, includes winner info

### Game Lifecycle

```
1. INVITATION
   - Player A sends GAME_INVITE to Player B
   - Server notifies Player B (PUSH message)

2. ACCEPTANCE
   - Player B sends GAME_INVITE_RESPONSE
   - Server creates Game instance
   - Notify both players

3. GAMEPLAY
   - Players alternate sending GAME_MOVE
   - Server processes moves via Game.processMove()
   - Updates game state
   - Sends responses to players

4. COMPLETION
   - Game determines winner
   - Server sends GAME_OVER PUSH to both players
   - Records match in leaderboard
   - Updates user stats

5. CLEANUP
   - Remove game from GameManager
   - Archive game data in user profiles
```

---

## Session Management

### Session Lifecycle

**Session Creation**:
1. User logs in with valid credentials
2. SessionManager creates new session token (UUID)
3. Session expires after configurable duration (e.g., 24 hours)

**Session Validation**:
1. Client includes sessionToken in message header
2. Server validates token exists and is not expired
3. If invalid: return `SESSION_EXPIRED` error

**Session Termination**:
1. Client sends LOGOUT message
2. Server invalidates session token
3. User must login again to continue

**Session Storage**:
```java
Map<UUID, Session> sessionMap  // sessionToken → Session
```

---

## Error Handling

### Exception Hierarchy

```
Throwable
├── Exception
│   ├── CommException (base for protocol errors)
│   │   ├── MessageParseException
│   │   └── MalformedMessageException
│   └── StoreException (data storage errors)
│       └── UsernameAlreadyTakenException
```

### Error Response Generation

**Flow**:
1. Handler catches exception or detects error condition
2. Determines appropriate ErrorCodeType
3. Creates error response message
4. Sends to client via Connection

**Example**:
```java
try {
    User user = userStore.registerUser(username, password);
} catch (UsernameAlreadyTakenException e) {
    Message errorResponse = messageBuilder.createErrorResponse(
        ErrorCodeType.USERNAME_TAKEN,
        "Username already in use"
    );
    connection.sendMessage(errorResponse);
}
```

---

## Configuration

### config.xml

**Typical Structure**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<server-configuration>
    <network>
        <default-port>5000</default-port>
        <backlog>50</backlog>
    </network>
    <session>
        <timeout-minutes>1440</timeout-minutes>
    </session>
    <logging>
        <level>INFO</level>
    </logging>
    <game>
        <max-concurrent-games>100</max-concurrent-games>
    </game>
</server-configuration>
```

**Configuration Loading**:
```java
ServerConfiguration.load()  // Called at startup
```

### System Properties

**Can be overridden via JVM arguments**:
```bash
java -Dserver.port=6000 iecd.a51597.server.Server
```

---

## Build & Deployment

### Maven Build

**Dependencies**:
```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.25.3</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.25.3</version>
</dependency>
```

**Build Command**:
```bash
mvn clean package
```

**Output**: `target/IECD-TP1-1.0-SNAPSHOT.jar`

### Project Configuration

**Java Version**: Java 25  
**Source Encoding**: UTF-8  
**Maven**: 3.6.0+

---

## Getting Started

### Prerequisites

- Java 25 installed
- Maven 3.6.0 or later
- TCP port 5000 available (or configure alternative)

### Building

```bash
cd C:\Users\rui\local-projects\FACULDADE\IECD\IECD-TP1
mvn clean compile
```

### Running the Server

**From source**:
```bash
mvn exec:java -Dexec.mainClass="iecd.a51597.server.Server"
```

**From compiled JAR**:
```bash
java -cp target/classes:. iecd.a51597.server.Server [port]
```

**With custom port**:
```bash
java -cp target/classes:. iecd.a51597.server.Server 6000
```

### Server Startup Output

```
[INFO] Initializing Server...
[INFO] Loading configuration from config.xml
[INFO] Loading users from data/users.xml
[INFO] Starting Listener thread with default port: 5000
[INFO] Server listening on port: 5000
[INFO] CLI Ready. Type 'help' for available commands.
```

### Interactive CLI

Once server is running:
```
server> help
Available commands:
  start [port]      - Start listening on port
  stop              - Stop listener
  status            - Show server status
  users             - List connected users
  kick <id>         - Disconnect user
  leaderboard       - Show top players
  shutdown          - Graceful shutdown
  help              - This help message

server> status
Server Status:
  Listening: Yes
  Port: 5000
  Connected Users: 0
  Active Games: 0

server> leaderboard
Top Players:
  1. alice (10 wins, 2 losses, 98.5% win rate)
  2. bob (8 wins, 3 losses, 72.7% win rate)

server> shutdown
Server shutdown complete
```

### Client Connection Example

Clients connect via TCP to `localhost:5000` (or configured port) and send/receive XML messages.

**Example Login Request**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<message type="REQUEST" id="550e8400-e29b-41d4-a716-446655440000" version="1.0">
    <header>
        <action>LOGIN</action>
        <timestamp>2026-04-20T12:30:45Z</timestamp>
    </header>
    <body>
        <username>alice</username>
        <password>secret123</password>
    </body>
</message>
```

---

## Logging

### Log Configuration

**File**: `src/main/resources/log4j2.xml`

**Log Files**:
- `logs/main.log`: Main server operations
- `logs/clients.log`: Client connection events
- `logs/protocol.log`: Protocol message details

**Log Levels**: DEBUG, INFO, WARN, ERROR

### Example Log Entries

```
[INFO] Server starting on port 5000
[INFO] User 'alice' logged in from 192.168.1.100:54321
[DEBUG] Processing GAME_MOVE for game a51597-game-001
[WARN] Session timeout for user bob
[ERROR] Database connection failed: Connection refused
```

---

## Development Guidelines

### Adding New Features

#### Adding a New Action Type

1. Add to `ActionType` enum
2. Create handler or extend existing handler
3. Update `MessageHandler` routing logic
4. Update protocol.xsd schema
5. Implement handler logic
6. Add tests

#### Adding a New Game

1. Implement `Game` interface
2. Implement `GameFactory` interface
3. Create `MoveCodec` for move encoding
4. Register with `GameManager`
5. Test game flow

#### Adding New Message Fields

1. Update `protocol.xsd`
2. Add fields to `MessageBody` nested classes
3. Update `XMLMessageBuilder` serialization
4. Update `XMLParser` deserialization
5. Update relevant handlers

### Code Organization

**Packages**:
- `iecd.a51597.server`: Server core
- `iecd.a51597.server.handlers`: Message handlers
- `iecd.a51597.server.game`: Game system
- `iecd.a51597.server.session`: Session management
- `iecd.a51597.server.store`: Data storage
- `iecd.a51597.common.protocol`: Communication protocol
- `iecd.a51597.client`: Client implementation

### Concurrency Considerations

**Thread-Safe Classes**:
- `Server` (singleton with proper synchronization)
- `UserStore` (synchronized maps/operations)
- `SessionManager` (thread-safe map)
- `Connection` (handles I/O in dedicated thread)

**Synchronization**:
- Use `synchronized` for shared resources
- Use concurrent collections when appropriate
- Avoid deadlocks (acquire locks in consistent order)
- Document thread safety in Javadoc

---

## Troubleshooting

### Common Issues

**Port Already in Use**
```
Error: Address already in use
Solution: Use different port: java ... Server 6000
```

**Session Timeout During Gameplay**
```
Error: SESSION_EXPIRED
Solution: Increase timeout in config.xml
```

**User Data Not Persisting**
```
Error: Users lost on restart
Solution: Check data/users.xml permissions, ensure save() called
```

**XML Parsing Errors**
```
Error: MessageParseException
Solution: Validate message against protocol.xsd
```

---

## Testing Checklist

- [ ] Server starts without errors
- [ ] Register new user
- [ ] Login existing user
- [ ] Search users
- [ ] Update profile
- [ ] Send game invitation
- [ ] Accept/decline invitation
- [ ] Play game moves
- [ ] Game ends correctly
- [ ] Leaderboard updates
- [ ] User data persists after shutdown
- [ ] Session expiration works
- [ ] Error handling for invalid inputs
- [ ] Concurrent user connections
- [ ] Server graceful shutdown

---

## References

### Related Files
- `protocol.xsd`: Protocol schema definition
- `config.xml`: Server configuration
- `data/users.xml`: Persisted user data
- `docs/protocol-reference.txt`: Protocol documentation
- `docs/TrabalhoPratico-PT.pdf`: Assignment specification

### Dependencies
- Apache Log4j 2.25.3: Logging framework

### Standards
- XML 1.0 with UTF-8 encoding
- UUID (RFC 4122)
- ISO 8601 timestamps
- ISO 3166-1 alpha-2 country codes

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-04-20 | Team | Initial comprehensive documentation |

---

**End of Documentation**

*For questions or clarifications, refer to the codebase comments and the project assignment document (TrabalhoPratico-PT.pdf)*

