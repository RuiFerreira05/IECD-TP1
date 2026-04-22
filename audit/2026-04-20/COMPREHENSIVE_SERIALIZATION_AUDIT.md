# Comprehensive Serialization/Deserialization Audit Report

**Audit Date:** April 20, 2026  
**Status:** ✅ MOSTLY SOUND (with one critical fix already applied)  
**Scope:** Full codebase analysis of all serialization/deserialization operations

---

## Executive Summary

The codebase has **good design patterns** with clear separation between:
- **Protocol Layer** - XML serialization for network communication
- **Persistence Layer** - XML serialization for data storage
- **Game Codec Layer** - Custom serialization for game moves
- **Configuration Layer** - XML serialization for settings

### Critical Issue Found & Fixed
✅ **NullPointerException in XMLMessageBuilder.userElement()** - FIXED

### Issues Identified
1. ✅ **FIXED** - NullPointerException risk in user serialization
2. ⚠️ **DESIGN ISSUE** - No validation that password parameter is hashed in updatePassword()
3. ⚠️ **DESIGN ISSUE** - UpdateProfile can receive plaintext password without hashing

---

## 1. Protocol Layer (Network Communication)

### Files Involved
- `XMLParser.java` - Deserializes incoming client messages
- `XMLMessageBuilder.java` - Serializes outgoing server responses
- `Message.java` - Protocol message record
- `MessageBody.java` - Sealed interface with message body types

### Serialization Flow

#### 1.1 Message Building (XMLMessageBuilder.java)

**Status:** ✅ CORRECT (AFTER FIX)

**What it serializes:**
- Response/Push messages in XML format
- User objects (for LOGIN and SEARCH responses)
- Game information (invites, moves, game over)
- Error messages with codes and descriptions

**Critical Methods:**

```java
// Lines 90-106: User Serialization (NOW FIXED)
private Element userElement(Document doc, User user) {
    Element userEl = doc.createElement("user");
    userEl.appendChild(textElement(doc, "id", user.getUserId().toString()));
    userEl.appendChild(textElement(doc, "username", user.getUsername()));
    if (user.getPhoto() != null) {
        userEl.appendChild(textElement(doc, "photo", user.getPhoto()));
    }
    if (user.getNationality() != null) {                          // ✅ FIXED
        userEl.appendChild(textElement(doc, "nationality", user.getNationality()));
    }
    if (user.getDob() != null) {                                 // ✅ FIXED
        userEl.appendChild(textElement(doc, "dob", user.getDob().toString()));
    }
    userEl.appendChild(playerStatsElement(doc, user.getStats()));
    return userEl;
}
```

**PlayerStats Serialization (Lines 104-116):** ✅ CORRECT
- Iterates through all match records
- Correctly serializes result (WON/LOST), playtime, opponent info
- No null safety issues

**Enum Serialization (ActionType, MessageType, ErrorCodeType):**
- ✅ Uses `.name()` for serialization (safe)
- Uses `fromString()` with normalization for deserialization

#### 1.2 Message Parsing (XMLParser.java)

**Status:** ✅ CORRECT

**What it deserializes:**
- Client REQUEST messages
- Header (action, session token, timestamp)
- Message body (varies by action)
- Schema validation against protocol.xsd

**Safe Parsing Techniques:**
```java
// Lines 111-116: Field validation
private String require(Element parent, String tag) throws MalformedMessageException {
    String value = getField(parent, tag);
    if (value == null)
        throw new MalformedMessageException("Missing required field: <" + tag + ">");
    return value;
}

// Lines 118-124: UUID parsing with error handling
private UUID requireUUID(Element parent, String tag) throws MalformedMessageException {
    try {
        return UUID.fromString(require(parent, tag));
    } catch (IllegalArgumentException e) {
        throw new MalformedMessageException("Invalid UUID in field <" + tag + ">", e);
    }
}

// Lines 133-137: Safe field extraction
private String getField(Element rootElement, String tag) {
    NodeList nodes = rootElement.getElementsByTagName(tag);
    if (nodes.getLength() == 0) return null;
    return nodes.item(0).getTextContent().trim();
}
```

**Schema Validation (Lines 139-145):**
- ✅ All parsed messages validated against protocol.xsd
- Prevents structural corruption
- Clear error reporting

**⚠️ IMPORTANT:** User objects are NEVER deserialized in protocol layer
- This is CORRECT - clients never send user objects
- User objects only flow from server → client

---

## 2. Persistence Layer (Data Storage)

### Files Involved
- `PersistenceManager.java` - Loads/saves users to data/users.xml
- `UserStore.java` - In-memory user store (backing store)
- Schema: `users.xsd`

### Load Flow (PersistenceManager.loadUsers)

**Status:** ✅ CORRECT

```java
// Lines 90-95: Reading user attributes
UUID userId = UUID.fromString(el.getAttribute("id"));
String username = el.getAttribute("username");
String passwordHash = el.getAttribute("passwordHash");
String photo = el.hasAttribute("photo") ? el.getAttribute("photo") : null;
String nationality = el.hasAttribute("nationality") ? el.getAttribute("nationality") : null;
LocalDate dob = el.hasAttribute("dob") ? LocalDate.parse(el.getAttribute("dob")) : null;
```

✅ **Safe:**
- Null checks for optional fields
- Proper date parsing with LocalDate.parse()
- Error handling with try-catch (line 122-124)

```java
// Lines 104-111: MatchRecord deserialization
NodeList matchNodes = statsEl.getElementsByTagName("match");
for (int j = 0; j < matchNodes.getLength(); j++) {
    Element matchEl = (Element) matchNodes.item(j);
    boolean won = "WON".equals(matchEl.getAttribute("result"));
    double playtime = parseDouble(matchEl.getAttribute("playtime"), 0.0);
    UUID oppId = UUID.fromString(matchEl.getAttribute("opponent-id"));
    String oppName = matchEl.getAttribute("opponent-username");
    matches.add(new PlayerStats.MatchRecord(won, playtime, oppId, oppName));
}
```

✅ **Safe:**
- Safe string comparison ("WON".equals(...))
- parseDouble with default value (line 193-198)
- All fields properly extracted

### Save Flow (PersistenceManager.saveUsers)

**Status:** ✅ CORRECT

```java
// Lines 154-159: Writing user attributes
userEl.setAttribute("id", user.getUserId().toString());
userEl.setAttribute("username", user.getUsername());
userEl.setAttribute("passwordHash", user.getPasswordHash());
if (user.getPhoto() != null) userEl.setAttribute("photo", user.getPhoto());
if (user.getNationality() != null) userEl.setAttribute("nationality", user.getNationality());
if (user.getDob() != null) userEl.setAttribute("dob", user.getDob().toString());
```

✅ **Safe:**
- All null fields are guarded with if statements
- Proper conversion to strings
- Match record serialization also properly guarded (lines 162-168)

**Schema Validation (Lines 174-179):**
- ✅ Generated XML validated against users.xsd before writing
- Prevents corruption

**Configuration Loading (ServerConfiguration.java):**

**Status:** ✅ CORRECT

- Schema validation applied to config.xml (lines 44-46)
- Safe string/int/long parsing with defaults (lines 65-88)
- Proper error handling with fallback to defaults

---

## 3. Game Move Codec Layer

### Files Involved
- `MoveCodec.java` - Interface for move serialization
- `GameHandler.java` - Uses codec
- `GameManager.java` - Manages codec

### Usage Pattern (GameHandler.gameMove)

**Status:** ✅ CORRECT

```java
// Lines 157-162: Safe deserialization with error handling
Move move;
try {
    move = gameManager.getCodec().deserialize(body.rawMove());
} catch (MalformedMessageException e) {
    sendError(message, connection, ErrorCodeType.MALFORMED_REQUEST, "Invalid move payload");
    return;
}
```

✅ **Safe:**
- Proper try-catch for deserialization errors
- Conversion to MALFORMED_REQUEST error code
- Move is only used after successful parsing

```java
// Lines 170-172: Safe serialization (just forwards raw string)
s.getConnection().sendMessage(
    messageBuilder.gameMovePush(game.getGameId(), body.rawMove())
);
```

✅ **Safe:**
- Raw move is passed through without modification
- Game codec handles any special encoding/decoding

---

## 4. Authentication & Security

### Password Handling

**Files:**
- `UserStore.java` - register(), findByCredentials(), updatePassword()
- `ProfileHandler.java` - updateProfile()

#### Register Flow (UserStore.register)

**Status:** ✅ CORRECT

```java
// Lines 20-31: Secure registration
public User register(String username, String password) throws UsernameAlreadyTakenException {
    UUID userId = UUID.randomUUID();
    String passwordHash = hash(password);  // ✅ Hashed immediately
    User user = new User(userId, username, passwordHash, null);
    // ... store user ...
}
```

#### Login Flow (UserStore.findByCredentials)

**Status:** ✅ CORRECT

```java
// Lines 46-53: Secure credential check
public Optional<User> findByCredentials(String username, String password) {
    User user = usernameIndex.get(username);
    String passwordHash = hash(password);  // ✅ Hashed for comparison
    if (user != null && user.getPasswordHash().equals(passwordHash)) {
        return Optional.of(user);
    }
    return Optional.empty();
}
```

#### ⚠️ ISSUE: UpdateProfile Password Handling

**File:** ProfileHandler.java, lines 33-34

```java
if (body.password() != null && !body.password().isBlank()) 
    userStore.updatePassword(user, body.password());
```

**Problem:** The password from the request is passed directly to updatePassword WITHOUT hashing!

**UserStore.updatePassword (Line 83-84):**
```java
public void updatePassword(User user, String newPasswordHash) {
    user.setPasswordHash(newPasswordHash);  // ❌ ASSUMES it's hashed, but it's NOT!
}
```

**Root Cause:** Method is named `updatePassword` but parameter is named `newPasswordHash`, suggesting it should receive a hash, but plaintext is being passed.

### 🔴 BUG FOUND: Password Not Hashed in Profile Update

**Severity:** CRITICAL  
**Location:** ProfileHandler.java line 34 + UserStore.updatePassword()  
**Impact:** Users can update their password to plaintext, bypassing security

**Fix Required:**
```java
// In ClientProfileHandler.updateProfile() - line 34
if (body.password() != null && !body.password().isBlank()) {
    String hashedPassword = hash(body.password());  // Hash it!
    userStore.updatePassword(user, hashedPassword);
}
```

Or better yet, add a hash method to UserStore and call it:
```java
// In UserStore
public void updatePassword(User user, String newPlaintextPassword) {
    user.setPasswordHash(hash(newPlaintextPassword));
}
```

---

## 5. Enum Serialization/Deserialization

### Files: ActionType.java, MessageType.java, ErrorCodeType.java

**Status:** ✅ CORRECT

**Pattern:** All use same safe approach
```java
public static ActionType fromString(String string) {
    String normalized = string.replace("-", "_").toUpperCase();
    try { 
        return ActionType.valueOf(normalized); 
    }
    catch (IllegalArgumentException e) { 
        return null; 
    }
}
```

✅ **Safe:**
- Handles format variations (kebab-case → UPPER_CASE)
- Returns null on invalid input (safe fallback)
- No exceptions thrown to caller

---

## 6. Network Frame Handling

### Files: Connection.java

**Status:** ✅ CORRECT

```java
// Lines 55-81: Safe message reading
private void readIncomingMessage() {
    try {
        int length = inputStream.readInt();
        
        if (length <= 0 || length > ServerConfiguration.MAX_FRAME_SIZE) {
            logger.warn("Invalid frame length {} from {}, closing", length, clientSocket.getInetAddress());
            closeConnection();
            return;
        }
        
        byte[] frameBytes = new byte[length];
        inputStream.readFully(frameBytes);
        messageDispatcher.handle(frameBytes, this);
    } catch (EOFException | SocketException e) {
        // Clean shutdown
        logger.info("Connection closed by {}", clientSocket.getInetAddress());
        closeConnection();
    }
}
```

✅ **Safe:**
- Frame size validation (prevents OOM attacks)
- Proper exception handling
- Clean resource cleanup

```java
// Lines 83-94: Safe message sending
public void sendMessage(byte[] payload) {
    if (payload == null) {return;}
    try {
        outputStream.writeInt(payload.length);
        outputStream.write(payload);
        outputStream.flush();
    } catch (IOException e) {
        logger.error("Error sending message to {}: {}", clientSocket.getInetAddress(), e.getMessage());
        closeConnection();
    }
}
```

✅ **Safe:**
- Null check on payload
- Proper framing (length prefix)
- Error handling with connection cleanup

---

## Summary Table

| Component | File | Issue | Severity | Status |
|-----------|------|-------|----------|--------|
| User Serialization (Protocol) | XMLMessageBuilder.java | NullPointerException on null nationality/dob | CRITICAL | ✅ FIXED |
| User Deserialization (Protocol) | XMLParser.java | N/A - not used (correct) | N/A | ✅ OK |
| User Persistence Load | PersistenceManager.java | None | N/A | ✅ OK |
| User Persistence Save | PersistenceManager.java | None | N/A | ✅ OK |
| Password Hashing (Register) | UserStore.java | None | N/A | ✅ OK |
| Password Hashing (Login) | UserStore.java | None | N/A | ✅ OK |
| Password Hashing (Update) | ProfileHandler + UserStore | Plaintext password not hashed | 🔴 CRITICAL | ❌ BROKEN |
| Game Move Codec | GameHandler.java | None | N/A | ✅ OK |
| Message Framing | Connection.java | None | N/A | ✅ OK |
| Configuration Load | ServerConfiguration.java | None | N/A | ✅ OK |
| Enum Serialization | ActionType, MessageType, ErrorCodeType | None | N/A | ✅ OK |

---

## Recommendations

### 1. 🔴 CRITICAL: Fix Password Hashing in Profile Update

**File:** ProfileHandler.java

**Current (BROKEN):**
```java
public void updateProfile(Message message, Connection connection) {
    // ...
    if (body.password() != null && !body.password().isBlank()) 
        userStore.updatePassword(user, body.password());  // ❌ Plaintext!
}
```

**Fixed:**
```java
public void updateProfile(Message message, Connection connection) {
    // ...
    if (body.password() != null && !body.password().isBlank()) {
        String hashedPassword = hash(body.password());  // Need to add hash() method
        userStore.updatePassword(user, hashedPassword);
    }
}
```

**Alternative Better Approach:**

Modify UserStore to handle hashing internally:

```java
// In UserStore.java
public void updatePassword(User user, String newPlaintextPassword) {
    user.setPasswordHash(hash(newPlaintextPassword));
}
```

Then in ProfileHandler:
```java
if (body.password() != null && !body.password().isBlank()) 
    userStore.updatePassword(user, body.password());  // ✅ Handled internally
```

### 2. Add Comprehensive Serialization Tests

```java
@Test
void testUserSerializationWithNullFields() {
    User userWithNulls = new User(UUID.randomUUID(), "test", "hash", null);
    // nationality and dob remain null
    
    byte[] serialized = builder.loginSuccess(messageId, sessionId, userWithNulls);
    
    // Should not throw NullPointerException ✅
    assertDoesNotThrow(() -> { /* parse and validate */ });
}

@Test
void testPasswordHashingOnUpdate() {
    User user = userStore.register("alice", "password123");
    String originalHash = user.getPasswordHash();
    
    userStore.updatePassword(user, "newPassword456");
    
    // Hash should have changed
    assertNotEquals(originalHash, user.getPasswordHash());
    
    // Password should be verifiable
    assertTrue(userStore.findByCredentials("alice", "newPassword456").isPresent());
    
    // Old password should NOT work
    assertFalse(userStore.findByCredentials("alice", "password123").isPresent());
}

@Test
void testPlayerStatsRoundTrip() {
    PlayerStats stats = new PlayerStats()
        .withMatch(true, 120.5, UUID.randomUUID(), "opponent1")
        .withMatch(false, 95.2, UUID.randomUUID(), "opponent2");
    
    User user = new User(UUID.randomUUID(), "test", "hash", null);
    user.setStats(stats);
    
    // Persist
    persistenceManager.save();
    
    // Reload
    userStore.loadUser(user);
    
    // Verify
    assertEquals(2, user.getStats().gamesPlayed());
    assertEquals(1, user.getStats().gamesWon());
}
```

### 3. Add Validation Logging

Add DEBUG logs to track serialization/deserialization:

```java
// In XMLMessageBuilder.userElement
logger.debug("Serializing user {} with nationality={}, dob={}", 
    user.getUserId(), user.getNationality(), user.getDob());

// In XMLParser.createMessage
logger.debug("Parsed message: type={}, action={}, version={}", 
    type, action, version);
```

### 4. Document Serialization Formats

Create a file documenting the three XML formats:

```
docs/SERIALIZATION_FORMATS.md
├── Protocol Format (protocol.xsd)
│   └── User as nested elements
├── Persistence Format (users.xsd)
│   └── User as attributes
└── Configuration Format (config.xsd)
    └── Server settings as elements
```

---

## Security Audit Summary

### ✅ Secure
- Password hashing on registration
- Password hashing on login verification
- Password hashes never transmitted in protocol
- Frame size validation prevents memory attacks
- Schema validation prevents injection attacks
- SQL injection not possible (XML-based storage)

### 🔴 Issues
- **CRITICAL:** Password not hashed in profile update (allows plaintext storage)

### ⚠️ Considerations
- Consider adding salt to password hashing (SHA-256 alone is vulnerable to rainbow tables)
- Consider bcrypt or Argon2 instead of raw SHA-256
- Session timeout is 30 minutes (reasonable)
- No rate limiting on login attempts

---

## Conclusion

**Overall Status:** ⚠️ MOSTLY SECURE (after password update fix)

The codebase demonstrates:
- ✅ Good separation of concerns (protocol, persistence, codec)
- ✅ Proper null safety in serialization (after fix)
- ✅ Comprehensive error handling
- ✅ Schema validation throughout
- ✅ Safe network framing

But requires:
- 🔴 **CRITICAL FIX:** Password hashing in profile updates

**Recommended Priority:**
1. 🔴 FIX password hashing (CRITICAL)
2. ⚠️ Enhance password hashing algorithm
3. 📋 Add comprehensive test suite
4. 📋 Document serialization formats


