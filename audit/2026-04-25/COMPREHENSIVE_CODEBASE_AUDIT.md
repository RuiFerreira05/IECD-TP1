# Comprehensive Codebase Audit

**Date:** 2026-04-25
**Auditor:** Automated static analysis
**Scope:** Full codebase — server, client, common, tests, build, config
**Total Findings:** 88

---

## Severity Legend

| Level | Meaning |
|-------|---------|
| **CRITICAL** | Will cause crashes, data corruption, or security breach in production |
| **HIGH** | Significant logic error, concurrency hazard, or design flaw |
| **MEDIUM** | Bad practice, anti-pattern, or maintainability concern |

---

## Table of Contents

1. [Critical Bugs](#1-critical-bugs)
2. [Critical Security Vulnerabilities](#2-critical-security-vulnerabilities)
3. [High Severity Concurrency Issues](#3-high-severity-concurrency-issues)
4. [High Severity Logic & Design Issues](#4-high-severity-logic--design-issues)
5. [Medium Severity — Protocol & Serialization](#5-medium-severity--protocol--serialization)
6. [Medium Severity — Client-Specific](#6-medium-severity--client-specific)
7. [Medium Severity — Server-Specific](#7-medium-severity--server-specific)
8. [Medium Severity — Architecture](#8-medium-severity--architecture)
9. [Test Weaknesses](#9-test-weaknesses)
10. [Build & Config Weaknesses](#10-build--config-weaknesses)

---

## 1. Critical Bugs

### 1.1 — `winner.toString()` emits garbage instead of UUID

**File:** `src/main/java/iecd/a51597/common/protocol/builders/server/XMLServerMessageBuilder.java:299`

**Problem:** `gameOverPush()` calls `winner.toString()` on a `User` object. Since `User` does not override `toString()`, this produces `iecd.a51597.server.store.entities.User@3b9a0c` — the default `Object.toString()` hash-based representation. The client receives this as the `winnerId` and calls `UUID.fromString()` on it, which throws `IllegalArgumentException`. Every game-over push crashes the client.

**Fix:** Replace `winner.toString()` with `winner.getUserId().toString()` to emit the actual UUID string.

---

### 1.2 — `getAge()` NPEs when `dob` is null

**File:** `src/main/java/iecd/a51597/common/store/UserDTO.java:14-15`

**Problem:** `dob.until(LocalDate.now())` throws `NullPointerException` for any user without a date of birth. The `dob` field is documented as nullable and is frequently null (e.g., users who skip DOB during registration).

**Fix:** Add a null guard:
```java
public int getAge() {
    if (dob == null) return -1; // or throw a custom checked exception
    return Period.between(dob, LocalDate.now()).getYears();
}
```
Alternatively, return `Optional<LocalDate>` for `dob()` and remove `getAge()` from the record, computing it at call sites.

---

### 1.3 — Tied game produces `GameOver(null)`, no Draw model

**File:** `src/main/java/iecd/a51597/common/game/DotsAndBoxesGame.java:56-60`

**Problem:** When scores are equal, `winnerId` is set to `null`. Downstream, `GameHandler` resolves `User` from this null UUID — causing NPE. Additionally, `MoveResult` is a sealed interface with no `Draw` variant, so the type system cannot represent a tie. The protocol has no way to communicate a draw to the client.

**Fix:**
1. Add a `Draw` variant to `MoveResult`: `record Draw(int p1Score, int p2Score) implements MoveResult {}`
2. When scores are equal, return `new Draw(...)` instead of `GameOver(null)`.
3. Add a `gameOverDraw` method to `ServerMessageBuilder` and handle the draw push in `GameHandler`.

---

### 1.4 — `sendMessage()` is not thread-safe

**File:** `src/main/java/iecd/a51597/server/network/Connection.java:101-111`

**Problem:** Multiple threads (game handler pushes, dispatcher responses) can write to the same `DataOutputStream` simultaneously. `writeInt()` + `write()` + `flush()` is not atomic — interleaved writes corrupt the 4-byte length prefix + payload framing. A client receiving a corrupted frame either misparses the payload or throws a protocol exception.

**Fix:** Synchronize all writes on the `DataOutputStream`:
```java
public void sendMessage(byte[] payload) {
    if (payload == null) return;
    synchronized (outputStream) {
        try {
            outputStream.writeInt(payload.length);
            outputStream.write(payload);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("Failed to send message", e);
            closeConnection();
        }
    }
}
```

---

### 1.5 — `connected = true` set BEFORE socket creation

**File:** `src/main/java/iecd/a51597/client/network/ServerConnection.java:108`

**Problem:** `connected` is set to `true` before `new Socket()` is called. If the socket constructor throws (e.g., network unreachable), `connected` remains `true` while no socket exists. Other threads calling `writeFrame()` attempt writes on null/closed streams, causing cascading NPEs.

**Fix:** Move `this.connected = true` to after the socket is successfully created and streams are opened. Use a try/catch that resets `connected = false` on failure.

---

### 1.6 — TOCTOU race on `pendingRequests`

**File:** `src/main/java/iecd/a51597/client/network/ServerConnection.java:130-132`

**Problem:** `containsKey()` + `remove()` is not atomic. If another thread (e.g., a timeout handler) removes the same key between the `containsKey` check and the `remove` call, `remove()` returns `null` and `.complete(message)` throws NPE.

**Fix:** Replace the two-step pattern with a single atomic operation:
```java
CompletableFuture<Message> future = pendingRequests.remove(correlationId);
if (future != null) {
    future.complete(message);
}
```

---

### 1.7 — `CommException` caught but read loop continues

**File:** `src/main/java/iecd/a51597/client/network/ServerConnection.java:155-157`

**Problem:** After a protocol/parse error, the loop continues reading from a potentially corrupted stream. There is no recovery mechanism — the stream position is unknown after a parse error, so subsequent reads will also fail. This produces an infinite loop of parse errors until the connection is eventually closed by the OS.

**Fix:** Break out of the read loop on `CommException`. Set `connected = false` and trigger the reconnect logic:
```java
catch (CommException e) {
    logger.error("Protocol error, disconnecting", e);
    this.connected = false;
    break;
}
```

---

### 1.8 — `MalformedMessageException` rethrown as `RuntimeException`, kills connection thread

**File:** `src/main/java/iecd/a51597/client/cli/screens/GameScreen.java:93-95`

**Problem:** A `MalformedMessageException` from a bad server push is caught, wrapped in `RuntimeException`, and rethrown. This crashes the entire connection reader thread. A single bad push (e.g., a malformed game state) permanently kills the client connection.

**Fix:** Log the error and continue processing. Do not rethrow:
```java
catch (MalformedMessageException e) {
    logger.error("Malformed game push received, ignoring", e);
}
```

---

### 1.9 — `StateMachineTest` cannot compile — entire test file is dead code

**File:** `src/test/java/StateMachineTest.java:18,20,32-33`

**Problem:** The test calls `sm.registerScreen(...)` and `sm.transitionTo(...)` which do not exist on `StateMachine`. The real API is `changeState(Screen)`. All 5 test methods reference these non-existent methods, so the file cannot compile. Zero actual test coverage exists for `StateMachine`.

**Fix:** Rewrite all 5 tests using the actual `StateMachine` API (`changeState()`, `back()`, `getCurrentScreen()`). Alternatively, if the API was intended to have `registerScreen`/`transitionTo`, add those methods to `StateMachine` and then verify the tests pass.

---

## 2. Critical Security Vulnerabilities

### 2.1 — SHA-256 without salt for password hashing

**File:** `src/main/java/iecd/a51597/server/store/UserStore.java:52-62`

**Problem:** Identical passwords produce identical hashes. No key stretching — modern GPUs can compute billions of SHA-256 hashes per second. Precomputed rainbow tables trivially reverse common passwords. A single leaked `users.xml` exposes all user credentials.

**Fix:** Use bcrypt or Argon2id via a library like `jbcrypt` or `argon2-jvm`:
```java
// Replace hash() with:
private static String hash(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt(12));
}
// Replace checkPassword() with:
boolean checkPassword(String input, String stored) {
    return BCrypt.checkpw(input, stored);
}
```
Note: This requires a migration plan for existing hashes.

---

### 2.2 — No TLS — passwords transmitted in plaintext

**File:** All networking code (`Connection.java`, `ServerConnection.java`, `ListenerThread.java`)

**Problem:** Raw TCP sockets with no transport encryption. Anyone with network access can sniff all credentials (passwords, session tokens) in transit. This includes WiFi, ISP-level, and LAN attackers.

**Fix:** Add an SSL/TLS layer using `SSLSocket` / `SSLServerSocket`. At minimum, provide a configuration option to enable TLS. For development, a self-signed cert is acceptable; for production, use proper PKI:
```java
// Server side:
SSLServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
ServerSocket serverSocket = ssf.createServerSocket(port);

// Client side:
SSLSocketFactory sf = SSLSocketFactory.getDefault();
Socket socket = sf.createSocket(host, port);
```

---

### 2.3 — XXE vulnerability in all XML parsing

**Files:** `XMLParser.java`, `XMLServerMessageBuilder.java`, `XMLClientMessageBuilder.java`, `ServerConfiguration.java`, `ClientConfiguration.java`, `XmlUserRepository.java`

**Problem:** All 6 `DocumentBuilderFactory.newInstance()` calls do not disable external entities or DTDs. A malicious client can send an XML payload like:
```xml
<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
<message><body>&xxe;</body></message>
```
This causes the server to read arbitrary files and include their contents in the response.

**Fix:** Add these features to every `DocumentBuilderFactory` instance:
```java
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
dbf.setXIncludeAware(false);
dbf.setExpandEntityReferences(false);
```
Extract this into a shared factory method in `common` to avoid duplication.

---

### 2.4 — Session hijacking — token not bound to connection

**File:** `src/main/java/iecd/a51597/server/handlers/BaseHandler.java:53-63`

**Problem:** `requireSession()` validates the session token but does not verify that the sending `Connection` matches the session's bound `Connection`. A stolen token (e.g., from an XXE response or network sniff) can be used from any TCP connection to impersonate the user.

**Fix:** After validating the token, verify the connection:
```java
Session session = sessionManager.getSession(token);
if (session == null || session.getConnection() != connection) {
    connection.sendMessage(messageBuilder.error(...));
    return null;
}
return session;
```
Also ensure `Session` stores a reference to the owning `Connection` and that this binding is set during login.

---

### 2.5 — No authentication required for user search

**File:** `src/main/java/iecd/a51597/server/handlers/SearchHandler.java:30-38`

**Problem:** Any unauthenticated connection can send a `SEARCH_USERS` request and enumerate all registered usernames by sending wildcard or short queries. This violates the principle of least privilege and enables targeted phishing or brute-force attacks.

**Fix:** Add `requireSession()` as the first step in `searchUsers()`:
```java
public void searchUsers(Message message, Connection connection) {
    Session session = requireSession(message, connection);
    if (session == null) return;
    // ... proceed with search
}
```

---

### 2.6 — No rate limiting on any endpoint

**Files:** All handler classes in `iecd.a51597.server.handlers`

**Problem:** Login brute-force, search enumeration, and game action spam are all unrestricted. An attacker can send thousands of login attempts per second to crack passwords.

**Fix:** Implement a simple rate limiter per connection or per IP. A token-bucket or sliding-window approach:
```java
public class RateLimiter {
    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public boolean allow(String key) {
        return attempts.computeIfAbsent(key, k -> new AtomicInteger(0))
                       .incrementAndGet() <= MAX_ATTEMPTS;
    }
}
```
Apply to `AuthHandler.login()`, `SearchHandler.searchUsers()`, and `GameHandler` actions.

---

### 2.7 — No input validation on any handler

**Files:** All handler classes, all client screens

**Problem:** Usernames, passwords, search queries, and profile fields have no length/character/format limits. This enables:
- DoS via very long strings (multi-MB username crashes XML serialization or exceeds `MAX_FRAME_SIZE`)
- Data corruption via XML-special characters (`<`, `>`, `&`) in usernames
- Injection via control characters

**Fix:** Add validation at the handler level:
```java
private void validateUsername(String username) {
    if (username == null || username.length() < 3 || username.length() > 32)
        throw new ValidationException("Username must be 3-32 characters");
    if (!username.matches("^[a-zA-Z0-9_]+$"))
        throw new ValidationException("Username contains invalid characters");
}
```
Apply equivalent validation to all user-supplied fields. The XSD schema should also enforce `minLength`/`maxLength`/`pattern` restrictions.

---

## 3. High Severity Concurrency Issues

### 3.1 — Fundamental thread-safety flaw in client Screen/StateMachine access

**Files:** `ServerConnection.java:140` + all Screen implementations

**Problem:** The network reader thread calls `handlePush()` on screens while the CLI thread calls `display()`/`handleInput()`. No synchronization exists on any `Screen` or `StateMachine` object. This causes:
- NPE if the network thread reads `currentScreen` while the CLI thread is transitioning
- Corrupted history stack if `back()` and `transitionTo()` interleave
- Interleaved console output from concurrent `display()` calls

**Fix:** Make `StateMachine.changeState()` and `back()` synchronized. Push `handlePush()` calls to the CLI thread via a queue:
```java
// In StateMachine:
private final Queue<Message> pushQueue = new ConcurrentLinkedQueue<>();

public void pushMessage(Message msg) {
    pushQueue.add(msg);
}

// In the CLI thread loop, before reading input:
while (!pushQueue.isEmpty()) {
    Message msg = pushQueue.poll();
    currentScreen.handlePush(msg);
}
```

---

### 3.2 — TOCTOU races in `isInGame` checks

**File:** `src/main/java/iecd/a51597/server/handlers/GameHandler.java:57,103,148`

**Problem:** Check-then-act is not atomic. Two simultaneous invites for the same player can both pass the `isInGame` check, allowing a player into multiple active games simultaneously.

**Fix:** Use `ConcurrentHashMap.putIfAbsent()` on `playerGameIndex` as the atomic check-and-reserve:
```java
if (playerGameIndex.putIfAbsent(playerId, gameId) != null) {
    // Player is already in a game — reject
}
```
This makes the check-and-occupy a single atomic operation.

---

### 3.3 — Lost-update race on `PlayerStats`

**File:** `src/main/java/iecd/a51597/server/handlers/GameHandler.java:348-349`

**Problem:** `user.setStats(user.getStats().withMatch(...))` is a read-modify-write without synchronization. If two game-over events for the same player execute concurrently, one reads the old stats, both compute new stats, and the second write overwrites the first — one match record is silently lost.

**Fix:** Use `ConcurrentHashMap.compute()` to atomically update:
```java
userMap.compute(userId, (id, user) -> {
    PlayerStats current = user.getStats();
    user.setStats(current.withMatch(matchResult));
    return user;
});
```
Or use an `AtomicReference<PlayerStats>` inside `User`.

---

### 3.4 — `createSession()` updates two maps non-atomically

**File:** `src/main/java/iecd/a51597/server/session/SessionManager.java:35-45`

**Problem:** `sessions.put()` and `userSessions.put()` are separate operations. A thread reading between them sees a session in `sessions` but not in `userSessions` (or vice versa), producing inconsistent state. Same issue in `invalidate()` (lines 78-84) where `sessions.remove()` and `userSessions.remove()` are separate.

**Fix:** Synchronize the pair of operations on a lock object:
```java
private final Object sessionLock = new Object();

public Session createSession(...) {
    synchronized (sessionLock) {
        Session session = new Session(...);
        sessions.put(session.getToken(), session);
        userSessions.put(userId, session.getToken());
        return session;
    }
}

public void invalidate(String token) {
    synchronized (sessionLock) {
        Session session = sessions.remove(token);
        if (session != null) {
            userSessions.remove(session.getUserId());
        }
    }
}
```
Read operations should also synchronize on the same lock, or use snapshot iteration.

---

### 3.5 — `register()` has non-atomic dual-map update

**File:** `src/main/java/iecd/a51597/server/store/UserStore.java:32-43`

**Problem:** `usernameIndex.putIfAbsent()` and `userMap.put()` are separate. A crash between them (or concurrent read) leaves the store inconsistent — the username appears taken but the user doesn't exist in the main map.

**Fix:** Perform both operations inside a single synchronized block on a lock object, or combine into one atomic check-and-put:
```java
private final Object storeLock = new Object();

public User register(String username, String password) {
    synchronized (storeLock) {
        if (usernameIndex.containsKey(username)) throw new UserExistsException();
        User user = new User(UUID.randomUUID(), username, hash(password));
        usernameIndex.put(username, user.getUserId());
        userMap.put(user.getUserId(), user);
        return user;
    }
}
```

---

### 3.6 — `updateUsername()` is not atomic

**File:** `src/main/java/iecd/a51597/server/store/UserStore.java:120-126`

**Problem:** Old username is removed and new one mapped in separate operations. There is a brief window where neither or both usernames resolve. A concurrent `searchByUsername()` or `login()` during this window gets inconsistent results.

**Fix:** Synchronize the username swap:
```java
synchronized (storeLock) {
    usernameIndex.remove(oldUsername);
    usernameIndex.put(newUsername, userId);
    user.setUsername(newUsername);
}
```

---

### 3.7 — `acceptGame()` is not atomic

**File:** `src/main/java/iecd/a51597/server/game/GameManager.java:65-75`

**Problem:** `pendingGames.remove()` + `activeGames.put()` + `playerGameIndex.put()` are all separate. A game can be in limbo between states — removed from pending but not yet in active. Concurrent queries for this game return null.

**Fix:** Synchronize the state transition:
```java
private final Object gameLock = new Object();

public DotsAndBoxesGame acceptGame(UUID gameId, User acceptor) {
    synchronized (gameLock) {
        DotsAndBoxesGame game = pendingGames.remove(gameId);
        if (game == null) return null;
        activeGames.put(gameId, game);
        playerGameIndex.put(game.getPlayer1Id(), gameId);
        playerGameIndex.put(game.getPlayer2Id(), gameId);
        return game;
    }
}
```

---

### 3.8 — `synchronized applyMove()` but unsynchronized getters

**File:** `src/main/java/iecd/a51597/common/game/DotsAndBoxesGame.java`

**Problem:** `getCurrentPlayerId()`, `isGameOver()`, `getDrawnLines()`, `getPlayer1Score()`, etc. are not synchronized. Multi-threaded access reads partially-updated state. For example, the score might be updated but `currentPlayer` not yet flipped, leading to a client seeing an inconsistent game state.

**Fix:** Either:
1. Make all getters `synchronized` (simple but can cause contention), or
2. Use `volatile` fields for single-value reads and `synchronized` blocks for compound reads, or
3. Return immutable snapshot objects from a `synchronized getState()` method.

---

### 3.9 — `getDrawnLines()` exposes mutable internal set

**File:** `src/main/java/iecd/a51597/common/game/DotsAndBoxesGame.java:43`

**Problem:** Returns a direct reference to the internal `HashSet`. Any code holding this reference can modify game state without calling `applyMove()`, bypassing all validation (turn checking, line-already-drawn checking, score calculation).

**Fix:** Return an unmodifiable view:
```java
public Set<Line> getDrawnLines() {
    return Collections.unmodifiableSet(drawnLines);
}
```
Or return a defensive copy: `return new HashSet<>(drawnLines);`

---

### 3.10 — `shutdownCompleted` is not volatile

**File:** `src/main/java/iecd/a51597/server/Server.java:47`

**Problem:** Written in `shutdown()` on one thread, read in the shutdown hook on another thread. Without `volatile`, the Java Memory Model does not guarantee the write is visible to the reading thread. The shutdown hook may loop forever waiting for a `shutdownCompleted` value it never sees.

**Fix:** Declare as `private volatile boolean shutdownCompleted = false;`

---

## 4. High Severity Logic & Design Issues

### 4.1 — Optimistic local move without rollback

**File:** `src/main/java/iecd/a51597/client/game/GameController.java:44,61`

**Problem:** A move is applied to the local game state before the server validates it. If the server rejects the move, the client game state is already mutated. The server rejection response is **completely ignored** (the future is created but never consumed). This causes a permanent client-server game state desync.

**Fix:** Either:
1. **Don't apply locally first** — wait for server confirmation before updating state, or
2. **Apply optimistically and rollback on rejection** — store the move and undo it if the server sends an error:
```java
// On rejection:
game.undoLastMove(); // implement undo logic
currentPlayerId = previousPlayerId;
```
3. At minimum, **consume the rejection future** and show an error to the user.

---

### 4.2 — Empty display + handleInput = user trapped on InvitePendingScreen

**File:** `src/main/java/iecd/a51597/client/cli/screens/InvitePendingScreen.java:25-32`

**Problem:** If the invite fails or the opponent never responds, the user is stuck on a blank screen forever. The `display()` method shows nothing, and `handleInput()` does nothing. There is no "back" or "cancel" option, and no timeout mechanism.

**Fix:**
1. Add a "Cancel" option in `display()` and handle it in `handleInput()`.
2. Add a timeout after which the screen auto-navigates back with a "Invite timed out" message.
3. Handle push messages for invite rejection.

---

### 4.3 — `GAME_OVER` push never handled by client

**File:** `src/main/java/iecd/a51597/client/cli/screens/GameScreen.java:88-96`

**Problem:** The client never processes the server's `GameOver` push. Game end is only detected locally by checking `isGameOver()` after each move. Any desync between client and server state means the client never knows the game ended. Combined with finding 1.1 (`winner.toString()` bug), this is a double failure: even if handled, the payload would crash the client.

**Fix:**
1. Add a `GAME_OVER` case in `handlePush()` that transitions to a results screen.
2. Fix the `winner.toString()` bug (finding 1.1) so the payload is parseable.
3. Display game results (scores, winner/draw) from the server's authoritative state.

---

### 4.4 — Player assignment assumption (inviter = Player1)

**Files:** `InvitePendingScreen.java:41-50`, `AnswerInviteScreen.java:29-38`

**Problem:** Both screens assume the inviter is always Player1. If the server assigns players differently (e.g., based on who accepted first, or randomly), `isMyTurn()` returns wrong values, making the game unplayable — the client thinks it's their turn when it's not, and vice versa.

**Fix:** The game state from the server should include each player's assigned ID. The client should use the server-assigned player ID rather than assuming:
```java
this.myPlayerId = game.getPlayer1Id().equals(sessionManager.getUser().userId())
    ? game.getPlayer1Id()
    : game.getPlayer2Id();
```
Or better: the server should include a `yourPlayerId` field in the game start push.

---

### 4.5 — Old socket never closed on reconnect

**File:** `src/main/java/iecd/a51597/client/network/ServerConnection.java:110-117`

**Problem:** When reconnecting, a new socket is created without closing the old one. This leaks file descriptors. On long-running clients with unstable connections, repeated reconnects exhaust the OS file descriptor limit, eventually preventing any new socket creation.

**Fix:** Close the old socket before creating a new one:
```java
private void closeExisting() {
    try {
        if (socket != null && !socket.isClosed()) socket.close();
    } catch (IOException ignored) {}
}
```
Call this at the start of the reconnect logic.

---

### 4.6 — `reconnectAttempts` never reset after successful connection

**File:** `src/main/java/iecd/a51597/client/network/ServerConnection.java:149-154`

**Problem:** The reconnect counter is decremented on each attempt but never reset to the configured maximum after a successful connection. After 3 cumulative disconnects across the entire session, the client permanently loses the ability to reconnect — even though the reconnect limit is intended per-connection, not per-session.

**Fix:** Reset `reconnectAttempts` to the configured maximum after a successful connect:
```java
this.connected = true;
this.reconnectAttempts = maxReconnectAttempts; // reset
```

---

### 4.7 — `shutdown()` only sets `connected = false`; does not close socket

**File:** `src/main/java/iecd/a51597/client/network/ServerConnection.java:161-163`

**Problem:** The read loop may block indefinitely on `inputStream.readInt()`. Setting `connected = false` does not unblock a blocking I/O call. No clean disconnect is possible — the thread only exits when the socket times out or the OS closes it.

**Fix:** Close the socket to unblock the read:
```java
public void shutdown() {
    this.connected = false;
    try {
        if (socket != null) socket.close();
    } catch (IOException ignored) {}
}
```
Also consider using `socket.setSoTimeout()` to ensure the read loop can check the `connected` flag periodically.

---

### 4.8 — Mutable `ArrayList` in `PlayerStats` record

**File:** `src/main/java/iecd/a51597/common/store/PlayerStats.java:28-30`

**Problem:** The record's constructor receives a `List<MatchRecord>` and stores it directly without a defensive copy. External code holding the original list reference can mutate the stats internally, breaking the record's immutability guarantee. Similarly, the accessor returns the same mutable list.

**Fix:** Defensive copy on construction and on access:
```java
public PlayerStats(..., List<MatchRecord> matchHistory) {
    this.matchHistory = List.copyOf(matchHistory); // immutable copy
}

// The auto-generated accessor already returns the field,
// so with List.copyOf it now returns an immutable list.
```

---

### 4.9 — `editProfile()` never updates from server response

**File:** `src/main/java/iecd/a51597/client/session/ClientSessionManager.java:54-61`

**Problem:** After sending an edit profile request, the client uses its own locally-constructed `UserDTO` instead of waiting for the server's response. If the server canonicalizes data (e.g., trimming whitespace, normalizing case, rejecting certain values), the client's local state diverges from the server's state.

**Fix:** Wait for the server response and use it to update the local user:
```java
public void editProfile(UserDTO updated) {
    Message response = connection.sendRequest(
        messageBuilder.editProfile(updated)
    ).get(5, TimeUnit.SECONDS);

    if (response.body() instanceof MessageBody.EditProfileSuccess s) {
        this.user = s.user(); // use server's canonical version
    }
}
```

---

### 4.10 — Edit Profile visibility uses reference equality (`==`)

**File:** `src/main/java/iecd/a51597/client/cli/screens/ViewProfileScreen.java:18,26`

**Problem:** The screen checks `sessionManager.getUser() == displayedUser` using `==` (reference equality). After editing the profile, `ClientSessionManager.user` is replaced with a new `UserDTO` instance. The `==` check now returns `false` even though it's the same logical user — the "Edit Profile" option disappears until the screen is recreated.

**Fix:** Use `.equals()` instead of `==`:
```java
if (sessionManager.getUser() != null
    && sessionManager.getUser().equals(displayedUser)) {
    // Show edit option
}
```
Ensure `UserDTO` has a proper `equals()` implementation (records auto-generate one).

---

### 4.11 — All `ServerConfiguration` fields are mutable `public static`

**File:** `src/main/java/iecd/a51597/server/config/ServerConfiguration.java:29-41`

**Problem:** Any code can change `MAX_FRAME_SIZE`, `SESSION_TIMEOUT_SECONDS`, etc. at any time during runtime. Tests that modify them must manually restore originals, and forgetting to do so corrupts other tests. This is a global mutable singleton anti-pattern.

**Fix:** Make all fields `private static final` and load them once from config:
```java
public final class ServerConfiguration {
    private static final int MAX_FRAME_SIZE;
    private static final int SESSION_TIMEOUT_SECONDS;
    // ...

    static {
        // Load from config.xml once
        ConfigData config = loadConfig();
        MAX_FRAME_SIZE = config.maxFrameSize;
        SESSION_TIMEOUT_SECONDS = config.sessionTimeout;
    }

    public static int getMaxFrameSize() { return MAX_FRAME_SIZE; }
    // ...
}
```
For tests that need to change config, use a `ServerConfiguration.setTestInstance(...)` method or dependency injection.

---

### 4.12 — Config values partially updated on failure

**File:** `src/main/java/iecd/a51597/server/config/ServerConfiguration.java:65`

**Problem:** If `parseInt` fails for one field, already-assigned fields keep their new values while the failed field falls back to default. The result is a mix of configured and default values with no rollback. The server runs with an inconsistent configuration that was never intended.

**Fix:** Parse all values into local variables first, validate them all, then apply atomically:
```java
public static void loadFromConfig(Element root) {
    int newPort = parseIntOrDefault(root, "port", DEFAULT_PORT);
    int newMaxFrame = parseIntOrDefault(root, "maxFrameSize", DEFAULT_MAX_FRAME);
    // ... parse all fields

    // Validate all
    if (newPort < 1 || newPort > 65535) throw new ConfigException("Invalid port");

    // Apply all at once
    SERVER_PORT = newPort;
    MAX_FRAME_SIZE = newMaxFrame;
    // ...
}
```

---

### 4.13 — Non-atomic file write; crash corrupts all user data

**File:** `src/main/java/iecd/a51597/server/persistence/XmlUserRepository.java:138`

**Problem:** `saveFrom()` writes directly to the target file (`data/users.xml`). A crash mid-write (power loss, OOM, disk full) produces a truncated or corrupted file. On restart, XML schema validation fails and the store loads zero users — all accounts are lost.

**Fix:** Write to a temporary file first, then atomically rename:
```java
public void saveFrom(UserStore store) {
    Path temp = persistencePath.resolveSibling("users.xml.tmp");
    try (OutputStream out = Files.newOutputStream(temp)) {
        // Write XML to temp file
        serialize(store, out);
    }
    Files.move(temp, persistencePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
}
```

---

### 4.14 — XML parsed before schema validation (billion laughs attack)

**File:** `src/main/java/iecd/a51597/server/persistence/XmlUserRepository.java:73-75`

**Problem:** The XML is fully parsed into a DOM before schema validation rejects it. A billion-laughs entity expansion attack causes OOM before the validator can reject the document. This is a DoS vector: a single malicious `users.xml` file crashes the server on startup.

**Fix:** Use a validating `DocumentBuilder` that validates during parsing:
```java
SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
Schema schema = sf.newSchema(schemaFile);
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setSchema(schema);
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
// Now parsing + validation happen in one pass
```

---

## 5. Medium Severity — Protocol & Serialization

### 5.1 — `ErrorCodeType.valueOf()` throws on hyphenated codes

**File:** `src/main/java/iecd/a51597/common/protocol/XMLParser.java:254`

**Problem:** `ErrorCodeType.valueOf()` is Java's auto-generated enum method — it requires exact case-match. If the wire format uses hyphenated or lowercase codes (e.g., `user-not-found`), this throws `IllegalArgumentException`. This is inconsistent with `ActionType.fromString()` which does case-insensitive matching.

**Fix:** Add a `fromString()` method to `ErrorCodeType` (like `ActionType` has) that handles case normalization and hyphenated formats. Replace all `valueOf()` calls with `fromString()`.

---

### 5.2 — Double `getField()` call for `dob`; uncaught `DateTimeParseException`

**File:** `src/main/java/iecd/a51597/common/protocol/XMLParser.java:207`

**Problem:** The `dob` field is extracted with two separate `getField()` calls. If the element exists but the value is not a valid ISO date, `LocalDate.parse()` throws `DateTimeParseException` — this is not caught. Also, calling `getField()` twice is redundant.

**Fix:** Call `getField()` once, and handle parse failures:
```java
String dobStr = getField(userElement, "dob");
LocalDate dob = (dobStr != null && !dobStr.isBlank())
    ? LocalDate.parse(dobStr)
    : null;
```
Use the existing `getLocalDate()` helper method if available.

---

### 5.3 — No explicit UTF-8 encoding on `Transformer` output

**Files:** `XMLServerMessageBuilder.java`, `XMLClientMessageBuilder.java`

**Problem:** The `Transformer` does not set an output encoding. On some platforms, the default encoding is not UTF-8, producing mojibake for non-ASCII usernames (e.g., accented characters, CJK). The `<?xml?>` declaration may also emit the wrong encoding attribute.

**Fix:** Set the encoding explicitly:
```java
transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
```
Also ensure the `ByteArrayOutputStream` is written with UTF-8 byte semantics.

---

### 5.4 — `getElementsByTagName()` ignores namespaces despite `namespaceAware=true`

**File:** `src/main/java/iecd/a51597/common/protocol/XMLParser.java:103-104`

**Problem:** `getElementsByTagName()` returns all elements with the given local name regardless of namespace. If `namespaceAware` is set to `true`, this means a malicious payload can inject elements in a different namespace that are still matched. This is an element injection vector.

**Fix:** Use namespace-aware methods:
```java
Element body = (Element) root.getElementsByTagNameNS(PROTOCOL_NS, "body").item(0);
```
This ensures only elements in the expected namespace are matched.

---

### 5.5 — `Boolean.parseBoolean()` silently converts anything to `false`

**File:** `src/main/java/iecd/a51597/common/protocol/XMLParser.java:137`

**Problem:** `Boolean.parseBoolean("maybe")` returns `false`. A malformed or unexpected value (e.g., "YES", "1", "maybe") in an invite acceptance field silently declines the invite. The user is never informed that their response was misinterpreted.

**Fix:** Validate the string explicitly and throw on unexpected values:
```java
private boolean parseStrictBoolean(String value) {
    if ("true".equalsIgnoreCase(value) || "yes".equals(value) || "1".equals(value)) return true;
    if ("false".equalsIgnoreCase(value) || "no".equals(value) || "0".equals(value)) return false;
    throw new CommException("Invalid boolean value: " + value);
}
```

---

### 5.6 — No compact constructor validation on `Message` record

**File:** `src/main/java/iecd/a51597/common/protocol/Message.java:18-24`

**Problem:** Null `messageId`, `messageType`, or `actionType` are silently accepted by the record constructor. These nulls propagate to serialization and cause NPEs at unpredictable points in the builder code.

**Fix:** Add a compact constructor:
```java
public Message {
    Objects.requireNonNull(messageId, "messageId must not be null");
    Objects.requireNonNull(messageType, "messageType must not be null");
    Objects.requireNonNull(actionType, "actionType must not be null");
}
```

---

### 5.7 — `UserMatch` record is dead code

**File:** `src/main/java/iecd/a51597/common/protocol/MessageBody.java:18`

**Problem:** `UserMatch` doesn't implement `MessageBody` and is never used anywhere in the codebase. It appears to be a leftover from an earlier design iteration.

**Fix:** Delete the `UserMatch` record, or integrate it into the protocol if search results need structured transport.

---

## 6. Medium Severity — Client-Specific

### 6.1 — `volatile Boolean` instead of `volatile boolean`

**File:** `src/main/java/iecd/a51597/client/Client.java:15`

**Problem:** `volatile Boolean` (boxed) permits `null` values. If any code path sets the field to `null`, unboxing causes NPE. The boxed type adds unnecessary object allocation on every read.

**Fix:** Change to `private volatile boolean running = true;`

---

### 6.2 — `sessionManager` field not volatile in `ServerConnection`

**File:** `src/main/java/iecd/a51597/client/network/ServerConnection.java:44`

**Problem:** `sessionManager` is read by the CLI thread and written by the network thread, but is not `volatile`. The Java Memory Model does not guarantee the CLI thread sees the updated reference.

**Fix:** Declare as `private volatile ClientSessionManager sessionManager;`

---

### 6.3 — No timeout on `answerInvite().get()` and `sendInvite().get()`

**File:** `src/main/java/iecd/a51597/client/invite/ClientInviteHandler.java:39,80`

**Problem:** `.get()` blocks indefinitely. If the server never responds (crash, network partition), the CLI thread hangs forever. The user cannot cancel or navigate away.

**Fix:** Use the timed version:
```java
Message response = future.get(30, TimeUnit.SECONDS);
```
Handle `TimeoutException` with a user-facing error and navigation back.

---

### 6.4 — `handlePush()` empty/TODO on most screens

**Files:** `LoginScreen.java`, `RegisterScreen.java`, `MainMenuScreen.java`, `GameMenuScreen.java`, etc.

**Problem:** Game invite pushes are silently dropped on screens that don't handle them. A user on the main menu never sees incoming invites. The push is lost, and the inviter's pending invite times out with no feedback.

**Fix:** Every screen should at minimum log unexpected pushes and/or queue them for display when a relevant screen becomes active. Consider a central push queue in `ClientCliHandler`:
```java
public void enqueuePush(Message push) {
    pendingPushes.add(push);
}
```
Screens can check the queue in `onEnter()`.

---

### 6.5 — "back" navigation uses `new SomeScreen()` instead of `sm.back()`

**Files:** `LoginScreen.java`, `SearchForPlayerScreen.java`, `SearchInviteScreen.java`

**Problem:** Creating a new instance of the previous screen instead of using `sm.back()` duplicates screens in the history stack. After navigating back and forward multiple times, the stack grows unboundedly and "back" no longer works intuitively (it revisits duplicate screens).

**Fix:** Replace `sm.changeState(new MainMenuScreen(...))` with `sm.back()` where appropriate. Reserve `changeState(new ...)` for forward navigation only.

---

### 6.6 — Copy-paste log message errors

**Files:** `SearchInviteScreen.java:41,46`, `InviteSearchResultsScreen.java:28,33`

**Problem:** Log messages in `SearchInviteScreen` say "SearchForPlayerScreen", and `InviteSearchResultsScreen` says "SearchResultsScreen". These are copy-paste errors that mislead debugging.

**Fix:** Update the logger strings to match the actual class names.

---

### 6.7 — Parameter named `Nationality` (uppercase N)

**File:** `src/main/java/iecd/a51597/common/protocol/builders/client/ClientMessageBuilder.java:67`

**Problem:** Violates Java naming convention (parameters use camelCase). This causes confusion and may produce inconsistent record field names if used in auto-generated code.

**Fix:** Rename to `nationality`.

---

### 6.8 — `resetState()` wipes ALL fields on `UsernameTaken`

**File:** `src/main/java/iecd/a51597/client/cli/screens/EditProfileScreen.java:89-103`

**Problem:** When the server responds with `UsernameTaken`, `resetState()` clears all fields — password, photo, nationality, DOB — even though only the username was invalid. The user must re-enter everything.

**Fix:** On `UsernameTaken`, only clear the username field. Preserve all other fields so the user only needs to pick a different username:
```java
case USERNAME_TAKEN -> {
    this.newUsername = null; // only clear username
    display();
}
```

---

## 7. Medium Severity — Server-Specific

### 7.1 — Port parameter not validated

**File:** `src/main/java/iecd/a51597/server/Server.java:183`

**Problem:** The port argument is not validated to be in range 1-65535. Port 0, negative values, or values > 65535 cause silent misbehavior (e.g., `ServerSocket(0)` picks a random port, negative values throw unclear exceptions).

**Fix:**
```java
int port = Integer.parseInt(args[0]);
if (port < 1 || port > 65535) {
    throw new IllegalArgumentException("Port must be between 1 and 65535");
}
```

---

### 7.2 — `connections` uses plain `ArrayList` with manual synchronization

**File:** `src/main/java/iecd/a51597/server/Server.java:46`

**Problem:** Manual `synchronized` blocks on `connections` are error-prone — forgetting to synchronize even one access causes a `ConcurrentModificationException`. The pattern is inconsistent (some accesses are synchronized, others may not be).

**Fix:** Use `CopyOnWriteArrayList<Connection>`:
```java
private final List<Connection> connections = new CopyOnWriteArrayList<>();
```
This eliminates the need for manual synchronization on reads and iterations.

---

### 7.3 — Unbounded thread creation per connection

**File:** `src/main/java/iecd/a51597/server/network/ListenerThread.java:42-44`

**Problem:** Each connection spawns a new thread with no naming, no daemon flag, and no thread pool. Under load, this creates thousands of threads, each consuming ~1MB of stack space. This causes OOM and makes debugging difficult (all threads are named "Thread-42").

**Fix:** Use a bounded `ExecutorService`:
```java
private static final ExecutorService pool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() * 2,
    new ThreadFactoryBuilder().setNameFormat("client-handler-%d").setDaemon(true).build()
);
```

---

### 7.4 — `invalidateByConnection()` does O(N) linear scan

**File:** `src/main/java/iecd/a51597/server/session/SessionManager.java:103-108`

**Problem:** Iterates over all sessions to find the one matching a given connection. With thousands of sessions, this is O(N) per disconnect. This is called on every disconnect, making server cleanup performance degrade linearly with user count.

**Fix:** Maintain a reverse index: `Map<Connection, String> connectionToToken`. This makes the lookup O(1).

---

### 7.5 — `User` entity is fully mutable with no validation guards

**File:** `src/main/java/iecd/a51597/server/store/entities/User.java:11-127`

**Problem:** `setUsername(null)`, `setPasswordHash(null)`, `setStats(null)` all silently corrupt state. There are no validation guards on any setter. The entity is an anemic data holder with no encapsulation.

**Fix:** Add validation to each setter:
```java
public void setUsername(String username) {
    Objects.requireNonNull(username, "username must not be null");
    if (username.length() < 3 || username.length() > 32)
        throw new IllegalArgumentException("Invalid username length");
    this.username = username;
}
```
Or better: make `User` immutable (record or final fields + builder pattern).

---

### 7.6 — `searchByUsername()` does O(N) full table scan

**File:** `src/main/java/iecd/a51597/server/store/UserStore.java:102-111`

**Problem:** Every search query iterates over all users. No index, no caching, and no result size limit. With thousands of users, a short query like "a" returns every user whose name contains "a" — potentially the entire database — on every keystroke.

**Fix:**
1. Add a `TreeMap<String, UUID>` with case-insensitive comparator for prefix-based lookup.
2. Limit results: `return results.stream().limit(20).toList();`
3. Consider caching popular queries with TTL.

---

### 7.7 — Partial profile update with no rollback

**File:** `src/main/java/iecd/a51597/server/handlers/ProfileHandler.java:46-49`

**Problem:** If username update succeeds but password update fails (e.g., hash function error), the username is already changed and cannot be rolled back. The user is left in a partially-updated state.

**Fix:** Validate all changes before applying any. Use a transactional approach:
```java
public void updateProfile(User user, ProfileUpdate update) {
    // Validate all fields first
    if (update.newUsername() != null) validateUsername(update.newUsername());
    if (update.newPassword() != null) validatePassword(update.newPassword());

    // Apply all changes
    if (update.newUsername() != null) user.setUsername(update.newUsername());
    if (update.newPassword() != null) user.setPasswordHash(hash(update.newPassword()));
    // ...
}
```

---

### 7.8 — Pending game doesn't add players to `playerGameIndex`

**File:** `src/main/java/iecd/a51597/server/handlers/GameHandler.java:114`

**Problem:** `createPendingGame` intentionally doesn't add players to `playerGameIndex`, which means the `isInGame` check doesn't catch pending invites. This allows a player to receive multiple pending invites simultaneously, creating ambiguity about which game they're joining.

**Fix:** Add players to `playerGameIndex` when creating a pending game, and remove them if the invite is declined or times out:
```java
playerGameIndex.put(inviterId, gameId);
playerGameIndex.put(inviteeId, gameId);
```
Add cleanup logic in decline/timeout paths.

---

### 7.9 — `orElseThrow()` can throw uncaught `NoSuchElementException`

**File:** `src/main/java/iecd/a51597/server/handlers/GameHandler.java:324-326`

**Problem:** If a user is deleted between game creation and game over (e.g., admin cleanup), `orElseThrow()` throws `NoSuchElementException` with no catch block. This crashes the game-over handler thread.

**Fix:** Use `orElse(null)` and handle the missing user:
```java
User winner = userMap.get(winnerId).orElse(null);
if (winner == null) {
    logger.warn("Winner {} not found, skipping stats update", winnerId);
    return;
}
```

---

### 7.10 — Non-atomic leaderboard snapshot

**File:** `src/main/java/iecd/a51597/server/game/Leaderboard.java:38-51`

**Problem:** Stats can be updated mid-stream while the leaderboard is being computed. This produces inconsistent rankings — player A's wins may come from one point in time while player B's come from another.

**Fix:** Take a snapshot of all user stats before sorting:
```java
public List<PlayerStats> getLeaderboard() {
    List<PlayerStats> snapshot = userStore.getAllUsers().stream()
        .map(User::getStats)
        .toList(); // immutable snapshot
    return snapshot.stream()
        .sorted(comparator)
        .limit(10)
        .toList();
}
```

---

### 7.11 — Error message uses wrong variable

**File:** `src/main/java/iecd/a51597/server/persistence/RepositoryFactory.java:12`

**Problem:** The default branch error message uses `ServerConfiguration.PERSISTENCE_TYPE` instead of the `type` parameter. If the user configured a bad type, the error message shows the default type (e.g., "xml") instead of the bad value they actually typed.

**Fix:** Replace `ServerConfiguration.PERSISTENCE_TYPE` with `type` in the error message:
```java
throw new IllegalArgumentException("Unknown persistence type: " + type);
```

---

### 7.12 — Constructor parameter `logger` is ignored (dead code)

**File:** `src/main/java/iecd/a51597/server/persistence/XmlUserRepository.java:40`

**Problem:** The constructor accepts a `Logger` parameter but never assigns it. The class uses its own static logger instead. The parameter is dead code that misleads callers into thinking they can customize logging.

**Fix:** Remove the `logger` parameter from the constructor, or assign it:
```java
public XmlUserRepository(Path persistencePath, Logger logger) {
    this.persistencePath = persistencePath;
    this.logger = logger; // actually use it
}
```

---

### 7.13 — `save()` is not synchronized in `PersistenceManager`

**File:** `src/main/java/iecd/a51597/server/persistence/PersistenceManager.java:38-39`

**Problem:** Concurrent save + user registration can produce inconsistent XML. The save iterates over users while a new user is being added. The serialized XML may contain a partial user or omit the new user entirely.

**Fix:** Synchronize `save()` with the same lock used for user registration, or take an immutable snapshot:
```java
public void save() {
    List<User> snapshot = userStore.getAllUsers(); // take snapshot under lock
    repository.saveFrom(snapshot); // serialize snapshot
}
```

---

## 8. Medium Severity — Architecture

### 8.1 — Circular dependency: `common` package imports `server` package

**File:** `src/main/java/iecd/a51597/common/protocol/builders/server/ServerMessageBuilder.java`, `src/main/java/iecd/a51597/common/game/GameFactory.java`

**Problem:** These files in the `common` package import `iecd.a51597.server.store.entities.User`. The "shared" package depends on the server package — the client cannot compile `common` independently. This violates the intended package layering.

**Fix:** Use `UserDTO` (which is already in `common`) instead of `User` in `ServerMessageBuilder` and `GameFactory`. Map from `User` to `UserDTO` at the server boundary (in the handlers).

---

### 8.2 — `Server.java` is a God class / singleton

**File:** `src/main/java/iecd/a51597/server/Server.java`

**Problem:** Holds 12+ mutable fields, serves as service locator, connection registry, lifecycle manager, and entry point. This makes the class impossible to test in isolation and creates tight coupling between all server components.

**Fix:** Decompose into focused classes:
- `ConnectionRegistry` — manages active connections
- `ServiceRegistry` — holds shared services (UserStore, SessionManager, GameManager)
- `ServerLifecycle` — startup/shutdown logic
- `ServerMain` — entry point only

Use dependency injection (constructor injection) to wire components.

---

### 8.3 — `MessageFactory` redundant with `ClientMessageBuilder`

**Files:** `MessageFactory.java`, `ClientMessageBuilder.java`

**Problem:** Two separate pathways for constructing client messages. They are not guaranteed to produce equivalent output. If one is updated and the other isn't, protocol violations occur silently.

**Fix:** Remove `MessageFactory` and use `ClientMessageBuilder` exclusively. Or merge them into a single class. Add integration tests that verify both produce identical XML for the same inputs (if both are kept).

---

### 8.4 — Hard-coded 5x5 grid dimensions

**File:** `src/main/java/iecd/a51597/common/game/DotsAndBoxesGame.java:12-13`

**Problem:** Grid size is hard-coded with no configurability. The `GameFactory` has no size parameter. Different game modes or difficulty levels are impossible without modifying source code.

**Fix:** Parameterize the grid size:
```java
public DotsAndBoxesGame(UUID player1Id, UUID player2Id, int rows, int cols) {
    this.rows = rows;
    this.cols = cols;
    // ...
}
```
Update `GameFactory.createGame()` to accept and pass through the dimensions.

---

## 9. Test Weaknesses

### 9.1 — Flaky test: `SESSION_TIMEOUT_SECONDS = 0` never restored

**File:** `src/test/java/SessionManagerTest.java:78`

**Problem:** `SESSION_TIMEOUT_SECONDS` is set to `0` and never restored. If this test runs before other session tests, all sessions expire immediately. Test pass/fail depends on execution order.

**Fix:** Use `@BeforeEach`/`@AfterEach` to save and restore the original value:
```java
private int originalTimeout;

@BeforeEach
void saveConfig() {
    originalTimeout = ServerConfiguration.SESSION_TIMEOUT_SECONDS;
}

@AfterEach
void restoreConfig() {
    ServerConfiguration.SESSION_TIMEOUT_SECONDS = originalTimeout;
}
```

---

### 9.2 — `USER_STORE` path not restored after test

**File:** `src/test/java/PersistenceManagerTest.java:27`

**Problem:** `USER_STORE` is pointed to a deleted temp directory. Subsequent tests that read `USER_STORE` reference a non-existent path, causing `NoSuchFileException` or loading zero users.

**Fix:** Save and restore `ServerConfiguration.USER_STORE` in `@BeforeEach`/`@AfterEach`, same pattern as 9.1.

---

### 9.3 — `assertDoesNotThrow` anti-pattern

**Files:** `XMLServerMessageBuilderTest.java:120-131`, `PersistenceManagerTest.java:80-97`

**Problem:** Wrapping assertions inside `assertDoesNotThrow` means if an exception occurs before the inner assertions, they are silently skipped and the test still appears to pass. The test provides a false sense of security.

**Fix:** Remove `assertDoesNotThrow` wrapper. Let the test method throw naturally — JUnit will catch and report it as a failure. If you need to assert no exception, put the logic directly in the test method body without the assertion wrapper.

---

### 9.4 — Zero test coverage for critical classes

**Problem:** The following critical classes have **zero** test coverage:
- `GameHandler.java` (399 lines of complex game logic)
- `GameManager.java` (game lifecycle)
- `DotsAndBoxesGame.java` (core game rules)
- `DotsAndBoxesMoveCodec.java` (serialization)
- `ProfileHandler.java` (all 5 update paths)
- `AuthHandler.java` (direct unit tests)
- `XMLClientMessageBuilder.java` (client-side serialization)
- `ClientSessionManager.java` (login/logout/edit flows)
- `GameController.java` (client game state)
- `ServerConnection.java` (TCP client connection)

**Fix:** Add unit tests for each class, prioritized by risk:
1. `DotsAndBoxesGame` — test win, draw, invalid moves, turn enforcement
2. `GameHandler` — test invite, accept, move, game-over flows
3. `AuthHandler` — test login, register, duplicate handling
4. `GameController` — test optimistic move, desync recovery
5. `ServerConnection` — test reconnect, frame parsing, timeout

---

### 9.5 — Partially tested classes with critical gaps

**Problem:**
- `XMLParserTest` — zero tests for RESPONSE and PUSH message parsing
- `MessageDispatcherTest` — 5 of 9 action types untested
- `PlayerStatsTest` — `winRate()` completely untested

**Fix:** Add tests for the missing coverage areas. For `XMLParserTest`, add test methods for parsing server responses and push messages. For `MessageDispatcherTest`, add tests for `SEARCH_USERS`, `EDIT_PROFILE`, `GET_LEADERBOARD`, `GAME_ACTION`, and `LOGOUT`. For `PlayerStatsTest`, add a `winRate()` test with 0/0, 3/10, and 10/10 cases.

---

### 9.6 — No `@Timeout` on any test

**Problem:** Tests involving blocking I/O (especially `ServerConnection`-related tests if added) could hang forever. JUnit 5 provides `@Timeout(value=5, unit=TimeUnit.SECONDS)` but none is used.

**Fix:** Add `@Timeout(5)` to any test that involves I/O, threads, or concurrency. Consider adding a project-wide default via `junit-platform.properties`:
```
junit.jupiter.execution.timeout.default=30 s
```

---

## 10. Build & Config Weaknesses

### 10.1 — Missing `junit-jupiter-engine` dependency

**File:** `pom.xml:30-34`

**Problem:** Only `junit-jupiter-api` is declared. Without `junit-jupiter-engine`, Maven Surefire may silently skip all tests on some environments (the engine is needed at runtime but not compile time). Tests may appear to pass with 0 tests run.

**Fix:** Add the engine dependency:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>${junit.version}</version>
    <scope>test</scope>
</dependency>
```

---

### 10.2 — No `maven-compiler-plugin` declared

**File:** `pom.xml:43-51`

**Problem:** Relies on the default compiler plugin version, which may not support Java 25 source/target. Different Maven versions ship different default compiler versions, causing build inconsistency across environments.

**Fix:** Explicitly declare the plugin:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <source>25</source>
        <target>25</target>
    </configuration>
</plugin>
```

---

### 10.3 — No Surefire fork or order config for order-dependent tests

**File:** `pom.xml:45-49`

**Problem:** Some tests are order-dependent (findings 9.1, 9.2). Without explicit configuration, Surefire's default behavior may run tests in a different order on different platforms, causing flaky CI failures.

**Fix:** Add Surefire configuration:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.0</version>
    <configuration>
        <forkCount>1</forkCount>
        <reuseForks>false</reuseForks>
    </configuration>
</plugin>
```
`reuseForks=false` ensures each test class runs in a fresh JVM, isolating static state.

---

### 10.4 — No log rotation

**File:** `src/main/resources/log4j2.xml:7-18`

**Problem:** All log files grow indefinitely. On a long-running server, `logs/server.log` can grow to gigabytes, filling the disk and potentially crashing the server.

**Fix:** Use `RollingFileAppender` instead of `FileAppender`:
```xml
<RollingFile name="MainFile" fileName="logs/server.log"
             filePattern="logs/server-%d{yyyy-MM-dd}-%i.log">
    <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"/>
    <Policies>
        <TimeBasedTriggeringPolicy interval="1"/>
        <SizeBasedTriggeringPolicy size="50MB"/>
    </Policies>
    <DefaultRolloverStrategy max="10"/>
</RollingFile>
```

---

### 10.5 — Root logger at WARN; common package has no dedicated logger

**File:** `src/main/resources/log4j2.xml:21-23`

**Problem:** The `iecd.a51597.common` package has no dedicated logger configuration. With the root at WARN, all INFO messages from the common package (protocol parsing, game logic, DTO operations) are invisible during normal operation. This makes diagnosing protocol issues very difficult.

**Fix:** Add a logger for the common package:
```xml
<Logger name="iecd.a51597.common" level="INFO" additivity="false">
    <AppenderRef ref="MainFile"/>
    <AppenderRef ref="ProtocolFile"/>
</Logger>
```

---

### 10.6 — Custom-named console logger bypasses standard log routing

**File:** `src/main/resources/log4j2.xml:25-27`

**Problem:** The `consoleLogger` is custom-named rather than following the standard `Console` appender pattern. Standard class-based loggers (via `LogManager.getLogger()`) never reach the console because the root logger only references `MainFile` and `NetworkFile`, not the console appender.

**Fix:** Add the console appender ref to the root logger:
```xml
<Root level="WARN">
    <AppenderRef ref="Console"/>
    <AppenderRef ref="MainFile"/>
    <AppenderRef ref="NetworkFile"/>
</Root>
```

---

### 10.7 — Client config XSD allows 0/negative port and reconnect attempts

**File:** `src/main/resources/schemas/client_config.xsd:7,10`

**Problem:** `serverPort` and `reconnectAttempts` use `xs:integer` which allows 0 and negative values. This is inconsistent with the server config which uses `xs:positiveInteger`. A port of 0 or -1 passes schema validation but fails at runtime.

**Fix:** Change to `xs:positiveInteger`:
```xml
<xs:element name="serverPort" type="xs:positiveInteger"/>
<xs:element name="reconnectAttempts" type="xs:positiveInteger"/>
```

---

### 10.8 — `persistenceType` is `xs:string` instead of enumeration

**File:** `src/main/resources/schemas/config.xsd:12`

**Problem:** Only "xml" is supported, but typos like "XML", "json", or "xml" pass schema validation and crash at runtime when `RepositoryFactory` can't match the type.

**Fix:** Use `xs:restriction` with an enumeration:
```xml
<xs:element name="persistenceType">
    <xs:simpleType>
        <xs:restriction base="xs:string">
            <xs:enumeration value="xml"/>
        </xs:restriction>
    </xs:simpleType>
</xs:element>
```

---

### 10.9 — No format restrictions on user-input fields in protocol XSD

**File:** `src/main/resources/schemas/protocol.xsd`

**Problem:** `username`, `password`, `nationality`, and `query` are all `xs:string` with no `minLength`, `maxLength`, or `pattern` restrictions. Empty passwords and 10MB search queries pass schema validation.

**Fix:** Add restrictions:
```xml
<xs:element name="username">
    <xs:simpleType>
        <xs:restriction base="xs:string">
            <xs:minLength value="3"/>
            <xs:maxLength value="32"/>
            <xs:pattern value="[a-zA-Z0-9_]+"/>
        </xs:restriction>
    </xs:simpleType>
</xs:element>
```

---

### 10.10 — `.gitignore` has stale patterns and potential data exposure

**File:** `.gitignore:2,7`

**Problem:** The `.mvn/wrapper` un-ignore pattern is stale (the wrapper directory is empty). More critically, `data/` is gitignored but `data/users.xml` with real unsalted password hashes may exist in working trees and could accidentally be committed to a public repository.

**Fix:**
1. Remove the stale `.mvn/wrapper` pattern.
2. Add an explicit `.gitignore` entry for `data/users.xml` with a comment warning about password hashes.
3. Add `data/users.xml` to `.gitattributes` as `diff=ignore` to prevent accidental leaking in diffs.

---

## Appendix: Priority Fix Order

If you can only fix 10 things, fix these first (ordered by impact):

| Priority | Finding | Impact |
|----------|---------|--------|
| 1 | 1.1 — `winner.toString()` | Every game-over crashes the client |
| 2 | 2.1 — SHA-256 without salt | All passwords trivially crackable |
| 3 | 1.4 — `sendMessage()` not thread-safe | Protocol corruption under concurrent writes |
| 4 | 2.3 — XXE vulnerability | Server file read by malicious clients |
| 5 | 1.3 — No Draw model | Tied game crashes server |
| 6 | 4.13 — Non-atomic file write | Power loss corrupts all user data |
| 7 | 2.2 — No TLS | Credentials sniffable on network |
| 8 | 1.2 — `getAge()` NPE | Crashes for users without DOB |
| 9 | 3.1 — Client thread-safety | Random client crashes |
| 10 | 1.9 — `StateMachineTest` dead code | Zero coverage for state machine |

---

*End of audit report — 88 findings total*
