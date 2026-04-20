# IECD-TP1: Developer Guide

## Quick Start for Developers

### Prerequisites
- Java 25 JDK
- Maven 3.6.0+
- IDE (IntelliJ IDEA, Eclipse, or VS Code with Java extensions)
- Git (for version control)

### Project Setup

1. **Clone/Open Project**
```bash
cd C:\Users\rui\local-projects\FACULDADE\IECD\IECD-TP1
```

2. **Build Project**
```bash
mvn clean compile
```

3. **Run Tests** (if applicable)
```bash
mvn test
```

4. **Start Development Server**
```bash
mvn exec:java -Dexec.mainClass="iecd.a51597.server.Server"
```

---

## Development Workflows

### Adding a New Message Action

**Scenario**: Add a new action type `CHANGE_STATUS` to allow users to set their status.

#### Step 1: Update ActionType Enum
```java
// File: src/main/java/iecd/a51597/common/protocol/types/ActionType.java
public enum ActionType {
    UNKNOWN,
    REGISTER,
    LOGIN,
    LOGOUT,
    UPDATE_PROFILE,
    SEARCH_USERS,
    CHANGE_STATUS,        // NEW
    GAME_INVITE,
    GAME_INVITE_RESPONSE,
    GAME_MOVE,
    GAME_OVER
}
```

#### Step 2: Update protocol.xsd
```xml
<!-- Add to ActionType enumeration in protocol.xsd -->
<xs:enumeration value="CHANGE_STATUS"/>

<!-- Add new status field to BodyType -->
<xs:element name="status-message" type="xs:string" minOccurs="0"/>
```

#### Step 3: Update MessageBody.java
```java
// Add nested class for CHANGE_STATUS
public static class ChangeStatus {
    public String statusMessage;    // "online", "offline", "away", or custom
}
```

#### Step 4: Create Handler (or extend existing)
```java
// File: src/main/java/iecd/a51597/server/handlers/StatusHandler.java
public class StatusHandler extends BaseHandler {
    private MessageBuilder messageBuilder;
    private SessionManager sessionManager;
    private UserStore userStore;
    
    public StatusHandler(MessageBuilder messageBuilder, 
                        SessionManager sessionManager,
                        UserStore userStore) {
        this.messageBuilder = messageBuilder;
        this.sessionManager = sessionManager;
        this.userStore = userStore;
    }
    
    @Override
    public boolean canHandle(ActionType action) {
        return action == ActionType.CHANGE_STATUS;
    }
    
    @Override
    public Message handle(Message message, Connection connection) {
        try {
            // Validate session
            if (message.sessionToken() == null) {
                return messageBuilder.createErrorResponse(
                    message.messageId(),
                    ErrorCodeType.NOT_AUTHENTICATED,
                    "Session required"
                );
            }
            
            if (!sessionManager.validateSession(message.sessionToken())) {
                return messageBuilder.createErrorResponse(
                    message.messageId(),
                    ErrorCodeType.SESSION_EXPIRED,
                    "Session expired"
                );
            }
            
            // Get user
            UUID userId = sessionManager.getUserId(message.sessionToken());
            User user = userStore.getUser(userId);
            
            // Update status
            String newStatus = message.body().getStatusMessage();
            user.setStatus(newStatus);
            userStore.updateUser(user);
            
            // Return success response
            return messageBuilder.createSuccessResponse(
                message.messageId(),
                message.actionType(),
                "Status updated successfully"
            );
            
        } catch (Exception e) {
            logger.error("Error handling CHANGE_STATUS", e);
            return messageBuilder.createErrorResponse(
                message.messageId(),
                ErrorCodeType.INTERNAL_ERROR,
                "Server error"
            );
        }
    }
}
```

#### Step 5: Register Handler in Server
```java
// File: src/main/java/iecd/a51597/server/Server.java
// In constructor, after other handlers:

StatusHandler statusHandler = new StatusHandler(messageBuilder, sessionManager, userStore);

// Add to MessageHandler
this.messageHandler = new MessageHandler(
    commParser, 
    messageBuilder, 
    authHandler, 
    profileHandler, 
    searchHandler, 
    gameHandler,
    statusHandler  // NEW
);
```

#### Step 6: Update MessageHandler Routing
```java
// File: src/main/java/iecd/a51597/server/handlers/MessageHandler.java
// In handle() method:

List<BaseHandler> handlers = Arrays.asList(
    authHandler,
    profileHandler,
    searchHandler,
    gameHandler,
    statusHandler  // NEW
);

for (BaseHandler handler : handlers) {
    if (handler.canHandle(message.actionType())) {
        return handler.handle(message, connection);
    }
}
```

#### Step 7: Test
```bash
# Build
mvn clean compile

# Test manually by sending XML message
```

---

### Creating a Custom Game Implementation

**Scenario**: Implement a simple "Guess the Number" game.

#### Step 1: Create Game Class
```java
// File: src/main/java/iecd/a51597/server/game/GuessTheNumberGame.java
package iecd.a51597.server.game;

import java.util.Random;
import java.util.UUID;

public class GuessTheNumberGame implements Game {
    private UUID gameId;
    private UUID player1Id;
    private UUID player2Id;
    private int secretNumber;
    private int currentGuess;
    private UUID winnerId;
    private boolean isFinished;
    
    private Random random = new Random();
    
    public GuessTheNumberGame(UUID gameId, UUID player1Id, UUID player2Id) {
        this.gameId = gameId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.secretNumber = random.nextInt(100) + 1;  // 1-100
        this.isFinished = false;
    }
    
    @Override
    public UUID getGameId() {
        return gameId;
    }
    
    @Override
    public GameType getType() {
        return GameType.GUESS_NUMBER;
    }
    
    @Override
    public MoveResult processMove(UUID playerId, String moveData) {
        // Parse the guess
        try {
            int guess = Integer.parseInt(moveData.trim());
            
            if (guess < 1 || guess > 100) {
                return new MoveResult.Rejected(
                    "Guess must be between 1 and 100"
                );
            }
            
            if (guess == secretNumber) {
                this.winnerId = playerId;
                this.isFinished = true;
                return new MoveResult.GameOver(winnerId);
            } else if (guess < secretNumber) {
                return new MoveResult.Accepted("Too low, try higher");
            } else {
                return new MoveResult.Accepted("Too high, try lower");
            }
            
        } catch (NumberFormatException e) {
            return new MoveResult.Rejected("Invalid guess format");
        }
    }
    
    @Override
    public boolean isFinished() {
        return isFinished;
    }
    
    @Override
    public UUID getWinnerId() {
        return winnerId;
    }
}
```

#### Step 2: Create Game Type Enum
```java
// File: src/main/java/iecd/a51597/server/game/GameType.java
package iecd.a51597.server.game;

public enum GameType {
    GUESS_NUMBER("Guess the Number"),
    CHESS("Chess"),
    TIC_TAC_TOE("Tic Tac Toe");
    
    private final String displayName;
    
    GameType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

#### Step 3: Create Game Factory
```java
// File: src/main/java/iecd/a51597/server/game/GuessTheNumberFactory.java
package iecd.a51597.server.game;

import java.util.UUID;

public class GuessTheNumberFactory implements GameFactory {
    
    @Override
    public Game createGame(UUID player1Id, UUID player2Id) {
        UUID gameId = UUID.randomUUID();
        return new GuessTheNumberGame(gameId, player1Id, player2Id);
    }
    
    @Override
    public GameType getGameType() {
        return GameType.GUESS_NUMBER;
    }
}
```

#### Step 4: Register Game in Server
```java
// File: src/main/java/iecd/a51597/server/Server.java
// In constructor, after messageHandler initialization:

// Register games
this.registerGameFactory(new GuessTheNumberFactory());
// registerGameFactory(new ChessFactory());
// registerGameFactory(new TicTacToeFactory());
```

#### Step 5: Test
```bash
# Build and run
mvn clean compile
mvn exec:java -Dexec.mainClass="iecd.a51597.server.Server"

# In another terminal, send game invitation and moves
```

---

### Adding Database Support

**Scenario**: Replace XML file persistence with database.

#### Step 1: Add Database Dependency
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.1.214</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>6.2.0</version>
</dependency>
```

#### Step 2: Create Database Persistence Manager
```java
// File: src/main/java/iecd/a51597/server/persistence/DatabasePersistenceManager.java
package iecd.a51597.server.persistence;

import iecd.a51597.server.store.User;
import iecd.a51597.server.store.UserStore;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DatabasePersistenceManager implements IPersistenceManager {
    private SessionFactory sessionFactory;
    private UserStore userStore;
    
    public DatabasePersistenceManager(UserStore userStore) {
        this.userStore = userStore;
        initializeSessionFactory();
    }
    
    private void initializeSessionFactory() {
        Configuration config = new Configuration();
        config.addAnnotatedClass(User.class);
        // Add mapping for other entities...
        this.sessionFactory = config.buildSessionFactory();
    }
    
    @Override
    public void load() {
        // Load users from database
        try (var session = sessionFactory.openSession()) {
            // Query all users and add to userStore
        }
    }
    
    @Override
    public void save() {
        // Save all users to database
        try (var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            // Save all users
            transaction.commit();
        }
    }
    
    @Override
    public void saveUser(User user) {
        // Save individual user
        try (var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            session.merge(user);
            transaction.commit();
        }
    }
}
```

#### Step 3: Switch Persistence Implementation
```java
// In Server constructor
// OLD:
// this.persistenceManager = new PersistenceManager(userStore);

// NEW:
this.persistenceManager = new DatabasePersistenceManager(userStore);
```

---

## Debugging Tips

### Enable Debug Logging
```xml
<!-- In src/main/resources/log4j2.xml -->
<Root level="DEBUG">
    <!-- Outputs to console and file -->
</Root>
```

### Debug Message Flow
```java
// Add debug output in Connection.java
@Override
public void run() {
    try {
        while (isConnected) {
            String xmlMessage = reader.readLine();
            logger.debug("Received message: {}", xmlMessage);
            
            Message message = commParser.parse(xmlMessage);
            logger.debug("Parsed message: action={}, type={}", 
                message.actionType(), message.messageType());
            
            Message response = messageHandler.handle(message, this);
            logger.debug("Generated response: {}", response);
            
            sendMessage(response);
        }
    } catch (Exception e) {
        logger.error("Connection error", e);
    }
}
```

### Using Breakpoints
In IDE, set breakpoints in:
- `Connection.run()`: Debug message reception
- `MessageHandler.handle()`: Debug routing
- Specific handlers: Debug action processing
- `UserStore.authenticate()`: Debug authentication

### Unit Testing
```java
// File: src/test/java/iecd/a51597/server/store/UserStoreTest.java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserStoreTest {
    
    @Test
    public void testUserRegistration() {
        UserStore store = new UserStore();
        
        User user = store.registerUser(
            "testuser",
            "password123",
            new UserProfile()
        );
        
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
    }
    
    @Test
    public void testDuplicateUsername() {
        UserStore store = new UserStore();
        
        store.registerUser("alice", "pass1", new UserProfile());
        
        assertThrows(UsernameAlreadyTakenException.class, () -> {
            store.registerUser("alice", "pass2", new UserProfile());
        });
    }
}
```

---

## Code Standards

### Naming Conventions
- **Classes**: PascalCase (e.g., `UserStore`, `MessageHandler`)
- **Methods**: camelCase (e.g., `getUserById`, `validateSession`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_PORT`, `MAX_CONNECTIONS`)
- **Variables**: camelCase (e.g., `userId`, `sessionToken`)
- **Packages**: lowercase (e.g., `iecd.a51597.server.handlers`)

### Documentation
```java
/**
 * Validates user session token.
 * 
 * @param sessionToken UUID of the session token
 * @return true if session is valid and not expired, false otherwise
 * @throws IllegalArgumentException if sessionToken is null
 */
public boolean validateSession(UUID sessionToken) {
    // Implementation
}
```

### Exception Handling
```java
try {
    // Operation that might fail
    User user = userStore.getUser(userId);
} catch (UserNotFoundException e) {
    logger.error("User not found: {}", userId, e);
    return messageBuilder.createErrorResponse(
        ErrorCodeType.USER_NOT_FOUND,
        "User does not exist"
    );
} catch (Exception e) {
    logger.error("Unexpected error retrieving user", e);
    return messageBuilder.createErrorResponse(
        ErrorCodeType.INTERNAL_ERROR,
        "Server error"
    );
}
```

### Thread Safety
```java
public class ThreadSafeUserStore {
    private final Map<UUID, User> users = new ConcurrentHashMap<>();
    
    // Use synchronized for compound operations
    public synchronized User registerUser(String username, String password) 
            throws UsernameAlreadyTakenException {
        if (userByUsername.containsKey(username)) {
            throw new UsernameAlreadyTakenException(username);
        }
        User user = new User(username, password);
        users.put(user.getId(), user);
        userByUsername.put(username, user);
        return user;
    }
}
```

---

## Performance Optimization

### Connection Pooling
```java
// For future database implementation
public class ConnectionPool {
    private static final int POOL_SIZE = 10;
    private Queue<Connection> availableConnections;
    
    public Connection getConnection() {
        // Get from pool or create new
    }
    
    public void releaseConnection(Connection conn) {
        // Return to pool
    }
}
```

### Caching
```java
public class UserStoreWithCache {
    private Map<UUID, User> userCache = new ConcurrentHashMap<>();
    private Map<String, UUID> usernameToIdCache = new ConcurrentHashMap<>();
    
    public User getUser(UUID userId) {
        return userCache.computeIfAbsent(userId, id -> {
            // Load from persistent store if not in cache
        });
    }
}
```

### Message Batching
```java
// For future optimization: batch multiple game moves
public class BatchedGameMoveProcessor {
    private Queue<Move> moveQueue;
    private static final int BATCH_SIZE = 10;
    
    public void processBatch() {
        List<Move> batch = new ArrayList<>();
        while (batch.size() < BATCH_SIZE && !moveQueue.isEmpty()) {
            batch.add(moveQueue.poll());
        }
        // Process batch together
    }
}
```

---

## Version Control

### Commit Message Format
```
[FEATURE/BUG/DOC] Brief description

Detailed explanation of changes and why they were made.

Fixes #123
```

### Branch Strategy
```
main                    - Production-ready code
├── dev                 - Development branch
│   ├── feature/xyz     - New feature
│   └── bugfix/abc      - Bug fix
```

---

## Deployment Checklist

- [ ] All tests passing
- [ ] Code review completed
- [ ] No debug logging left enabled
- [ ] Configuration optimized for production
- [ ] Documentation updated
- [ ] Users.xml backed up
- [ ] Port availability verified
- [ ] Firewall rules configured
- [ ] Monitor logs during initial startup

---

**End of Developer Guide**

For questions or issues, check the codebase comments and the main CODEBASE_DOCUMENTATION.md file.

