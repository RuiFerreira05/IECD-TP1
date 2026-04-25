# TODO — Missing & Incomplete Features

Cross-reference of the assignment requirements against the current codebase.
Each item includes the requirement, current status, and implementation guidance.

---

## 1. User Registration — Missing Photo, Nationality, Age at Signup

**Requirement:** "Um jogador faz o seu auto registo indicando uma alcunha (nickname), uma senha (password) e uma fotografia que pode alterar posteriormente. Sobre cada jogador é necessário conhecer a nacionalidade, a idade…"

**Status:** Registration only collects `username` + `password`. Photo, nationality, and date-of-birth are always `null` at registration and can only be set later via profile edit.

**Implementation:**

1. **Extend `MessageBody.Register`** to include `photo`, `nationality`, `dob` fields (all optional/nullable):
   ```java
   record Register(String username, String password, String photo, String nationality, LocalDate dob)
   ```
2. **Update `XMLClientMessageBuilder.register()`** to write the new fields when non-null.
3. **Update `XMLParser`** to extract `photo`, `nationality`, `dob` from the register body.
4. **Update `AuthHandler.register()`** to pass the new fields to `UserStore.register()`, which must be extended to accept them and set them on the `User` object.
5. **Update `UserStore.register()`** signature to `register(username, password, photo, nationality, dob)`.
6. **Update `RegisterScreen`** to prompt for the new fields (with "skip" option).
7. **Update `protocol.xsd`** to add the optional elements to the register body.

---

## 2. Photo Display in Profile View

**Requirement:** Photo is part of the player profile and should be visible.

**Status:** The `photo` field exists in `User`, `UserDTO`, protocol, and persistence, but `ViewProfileScreen` never displays it. Even the URL/string value is not printed.

**Implementation:**

1. **In `ViewProfileScreen`**, add a line after username:
   ```java
   if (user.photo() != null && !user.photo().isBlank()) {
       System.out.println("  Photo: " + user.photo());
   }
   ```
2. For a richer CLI experience, consider displaying a small ASCII-art placeholder or truncated URL. For a future GUI, the photo URL could be used to fetch and render the image.

---

## 3. Leaderboard — No Client Access

**Requirement:** The assignment values innovative features; a leaderboard is a natural extension of the "registo de vitórias, derrotas" tracking.

**Status:** `Leaderboard` class exists on the server, but there is **no `ActionType.LEADERBOARD`**, no protocol message, no handler, and no client screen.

**Implementation:**

1. **Add `LEADERBOARD` to `ActionType` enum.**
2. **Add `LeaderboardRequest` / `LeaderboardResponse` to `MessageBody`:**
   ```java
   record LeaderboardRequest(int limit) {}
   record LeaderboardEntry(String username, int gamesWon, int gamesLost, double totalPlayTimeSecs) {}
   record LeaderboardResponse(List<LeaderboardEntry> entries) {}
   ```
3. **Add `LeaderboardHandler`** (or extend `ProfileHandler`) — query `Leaderboard.getTopPlayers(limit)`, build response.
4. **Update `MessageDispatcher`** to route `LEADERBOARD` to the handler.
5. **Update `XMLServerMessageBuilder`** to serialize leaderboard entries.
6. **Update `XMLParser`** to parse leaderboard response.
7. **Add `LeaderboardScreen`** on the client with a menu option in `MainMenuScreen`.
8. **Update `protocol.xsd`** with the new action and body elements.

---

## 4. Box Ownership Visualization on Board

**Requirement:** "A caixa conquistada é marcada com a inicial ou cor do jogador." (Rule 3.2)

**Status:** `ClientBoardRenderer` only shows lines and dots. Captured boxes are not visually marked with player initials or any identifier. Only the score line reflects progress.

**Implementation:**

1. **Extend `DotsAndBoxesGame`** to track **which player** owns each box. Currently `capturedBoxes` is a `Set<String>` of `"x,y"` keys with no owner info. Change to:
   ```java
   Map<String, UUID> capturedBoxOwners = new HashMap<>(); // "x,y" -> playerId
   ```
   Update `captureBox(x, y, playerId)` to store the owner. Update `checkAndCaptureBoxes` accordingly.
2. **Add a method** to query the owner of a box: `getBoxOwner(int x, int y) -> Optional<UUID>`.
3. **Update `ClientBoardRenderer.printBoard()`** to display the owner's initial in the center of each captured box. For a box at `(x, y)`, the center is between row `y` and `y+1`, column `x` and `x+1`. Render as `A ` or `B ` (first letter of each player's username), or leave blank if uncaptured.
4. **Pass player usernames** (or initials) to the renderer so it can label boxes.

---

## 5. Bonus Turn Notification

**Requirement:** "Ao fechar uma caixa, o jogador tem de realizar imediatamente outra jogada." (Rule 3.3)

**Status:** The bonus turn logic is correctly implemented — `currentPlayerId` stays the same after a capture. However, **the client provides no notification** that a bonus turn was earned. The player sees "Your turn!" again with no explanation.

**Implementation:**

1. **`MoveResult.Accepted`** should indicate whether a box was captured. Currently it's a sealed interface with no fields. Add:
   ```java
   record Accepted(boolean capturedBox) implements MoveResult {}
   ```
2. **Update `DotsAndBoxesGame.applyMove()`** to return `new Accepted(capturedAnyBox)`.
3. **In `GameController.attemptLocalMove()`**, check the `MoveResult` — if `Accepted(capturedBox=true)`, print a message like:
   ```
   ★ You captured a box! Go again!
   ```
4. **In `GameScreen.handlePush()`**, when applying an opponent's move, show:
   ```
   [Opponent captured a box! They play again.]
   ```

---

## 6. Draw/Tie Handling — Stats Bug & GameOver Push NPE

**Requirement:** Game must correctly record outcomes. Currently draws are recorded as losses for both players.

**Status:**
- `MatchRecord` only has `boolean won` — no DRAW state.
- When `winnerId == null`, both `p1Won` and `p2Won` are `false` → both players get a LOST record.
- `XMLServerMessageBuilder.gameOverPush()` NPEs when `winner == null` (draw game).

**Implementation:**

1. **Add `DRAW` result to `MatchRecord`:** Replace `boolean won` with an enum:
   ```java
   enum Result { WON, LOST, DRAW }
   record MatchRecord(Result result, double playtimeSecs, UUID opponentId, String opponentUsername)
   ```
2. **Update `PlayerStats`**: `gamesWon()` counts `WON`, `gamesLost()` counts `LOST`, add `gamesDrawn()` counting `DRAW`.
3. **Update `GameHandler`** game-over logic:
   ```java
   Result p1Result = winner == null ? Result.DRAW 
                     : (winner.getUserId().equals(p1.getUserId()) ? Result.WON : Result.LOST);
   Result p2Result = winner == null ? Result.DRAW 
                     : (winner.getUserId().equals(p2.getUserId()) ? Result.WON : Result.LOST);
   ```
4. **Update `XmlUserRepository`** persistence: add `DRAW` as a valid `result` attribute value (`"DRAW"`). Update `users.xsd` `MatchResultType` to include `DRAW`.
5. **Fix `XMLServerMessageBuilder.gameOverPush()`**: Handle `winner == null` (draw) — either omit `winner-id`/`winner-username` elements or use a sentinel value.
6. **Update `protocol.xsd`** `MatchResultType` to add `DRAW`.

---

## 7. Board Size Configurability

**Requirement:** "Uma grelha retangular de pontos (ex: 3 X 3, 5 X 5)." — The assignment explicitly gives examples of different sizes.

**Status:** Board size is **hardcoded** as `WIDTH = 5, HEIGHT = 5` in both `DotsAndBoxesGame` and `ClientBoardRenderer`. Not configurable.

**Implementation:**

1. **Make `DotsAndBoxesGame` dimensions instance fields** (not `static final`):
   ```java
   private final int width, height;
   ```
   Update constructor, `isGameOver()`, `isValidLine()`, `checkAndCaptureBoxes()`, and `checkBoxClosed()` to use instance fields.
2. **Update `DotsAndBoxesGameFactory`** to accept dimensions:
   ```java
   DotsAndBoxesGameFactory(int width, int height)
   ```
3. **Add board size to `GAME_INVITE` or `GAME_INVITE_RESPONSE`** protocol so both clients agree on the size. Add `boardWidth` / `boardHeight` fields to the game-start push/response messages.
4. **Update `ClientBoardRenderer`** to accept dimensions as parameters instead of hardcoded constants.
5. **Optionally add board size to `ServerConfiguration`** as defaults (e.g., `defaultBoardWidth`, `defaultBoardHeight`).
6. **Add a board-size selection step** in the invite flow (e.g., inviter chooses size, invitee sees it before accepting).

---

## 8. Game Forfeiture / Resignation

**Requirement:** Players should be able to exit a game before completion (implied by usability / "facilidades de utilização").

**Status:** No protocol action or UI for forfeiting. The only way to end a game is completing all boxes. Disconnecting leaves an orphaned game.

**Implementation:**

1. **Add `GAME_RESIGN` to `ActionType` enum.**
2. **Add `MessageBody.GameResign`**: `record GameResign(UUID gameId) {}`
3. **Add handler in `GameHandler`**: on resign, the resigning player loses, the opponent wins. Update stats, push `GAME_OVER` to both players (with winner = opponent).
4. **Update `GameManager`** to handle mid-game cleanup on resignation.
5. **Update `GameScreen`** to offer a "Resign" option (e.g., type `resign` instead of coordinates).
6. **Update `protocol.xsd`** with the new action.

---

## 9. Disconnection / Reconnection Handling

**Requirement:** Robustness / "tolerância às falhas" (mentioned in report requirements).

**Status:** If a player disconnects mid-game, the game stays in `activeGames` forever. The opponent is stuck. No reconnection mechanism exists.

**Implementation:**

1. **Detect disconnect during active game**: In `Connection.closeConnection()`, check if the disconnected user has an active game (via `GameManager.playerGameIndex`). If so, start a grace period (e.g., 60 seconds).
2. **Add a reconnection mechanism**: On reconnect + re-login, check if the user has a game in `GameManager.playerGameIndex`. If so, restore the game state instead of creating a new session.
3. **Add timeout**: If the disconnected player doesn't reconnect within the grace period, auto-forfeit (see item 8) — award the win to the remaining player.
4. **Notify the remaining player**: Push a "opponent disconnected, waiting for reconnect…" message, then a `GAME_OVER` push if the timeout expires.

---

## 10. Incremental Persistence (Save After Each Game)

**Requirement:** Data durability — game results should not be lost on server crash.

**Status:** `PersistenceManager.save()` only runs at graceful shutdown. A crash loses all game results and profile changes since the last save.

**Implementation:**

1. **Call `persistenceManager.save()` after each game ends** (in `GameHandler` after stats update):
   ```java
   server.getPersistenceManager().save();
   ```
2. **Also save after profile updates** (in `ProfileHandler.updateProfile()` on success).
3. **Consider a periodic auto-save** (e.g., every 5 minutes) via a scheduled thread:
   ```java
   ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
   scheduler.scheduleAtFixedRate(() -> persistenceManager.save(), 5, 5, TimeUnit.MINUTES);
   ```

---

## 11. Real-Time Invite Notification

**Requirement:** Usability — players should know when they receive an invite without manually checking.

**Status:** `MainMenuScreen.handlePush()` is `// TODO`. Push notifications (invites) are silently added to `pendingInvites` but never shown to the user in real-time.

**Implementation:**

1. **Implement `handlePush()` in `MainMenuScreen`**: When a `GAME_INVITE` push arrives, print:
   ```
   [!] You received a game invite from <username>! Go to Play > View Invites to respond.
   ```
2. **Implement `handlePush()` in other screens** (Login, Register, etc.) similarly, or at minimum show a notification line.
3. **Add a notification banner mechanism** to `Screen` base class that persists across screen transitions.

---

## 12. Cancel Pending Invite

**Requirement:** Usability — the inviter should be able to cancel an invite or go back while waiting.

**Status:** `InvitePendingScreen` has empty `display()` and `handleInput()`. The user is stuck until the opponent responds or the connection drops.

**Implementation:**

1. **Add a "Cancel" option in `InvitePendingScreen`**: Allow typing `cancel` or `back`.
2. **Add `CANCEL_INVITE` to `ActionType`** (or reuse `GAME_INVITE_RESPONSE` with a cancel flag).
3. **Update `GameManager`** to support cancellation of pending games.
4. **Push a notification to the invitee** that the invite was cancelled.

---

## 13. GAME_OVER Push Handling on Client

**Requirement:** Correct protocol compliance — server pushes `GAME_OVER` but client ignores it.

**Status:** `GameScreen.handlePush()` only handles `GAME_MOVE`. The `GAME_OVER` push from the server is silently dropped. Game-over is detected only via local `isGameOver()`, which works but is fragile.

**Implementation:**

1. **In `GameScreen.handlePush()`**, add a case for `GAME_OVER`:
   ```java
   case GAME_OVER -> {
       // Extract winner info from push body
       // Display result (WIN/LOSE/TIE)
       // Set a flag to prevent further input
       // Wait for Enter to return to MainMenu
   }
   ```

---

## 14. Stale Invite List Cleanup

**Status:** `Client.pendingInvites` is a `CopyOnWriteArrayList` that never removes entries. Accepted/declined invites remain in the list forever, so "View Invites" shows stale entries.

**Implementation:**

1. **In `ClientInviteHandler.answerInvite()`**, remove the invite from `pendingInvites` after successful accept/decline (partially done — verify it works for all paths).
2. **In `InvitePendingScreen.handlePush()`**, when the invite response push arrives (accepted/declined), remove the corresponding invite from the list.
3. **In `ViewInvitesScreen`**, refresh the list from `client.getPendingInvites()` on `onEnter()` instead of snapshotting at construction time.

---

## 15. Connection.sendMessage() Thread Safety

**Status:** `Connection.sendMessage()` on the server side is **not synchronized**. If two threads send on the same connection concurrently (e.g., a push notification while a response is being written), the 4-byte length prefix and payload can interleave, corrupting the stream.

**Implementation:**

1. **Add `synchronized` to `Connection.sendMessage()`**:
   ```java
   public synchronized void sendMessage(byte[] payload) { ... }
   ```
   This matches the client-side `ServerConnection.writeFrame()` which is already `synchronized`.

---

## 16. Server-Side `gameOverPush` NPE Fix

**Status:** `XMLServerMessageBuilder.gameOverPush()` calls `winner.toString()` and `winner.getUsername()` which throws `NullPointerException` when `winner == null` (draw game). Also uses `winner.toString()` instead of `winner.getUserId().toString()` for the `<winner-id>` element.

**Implementation:**

1. **Null-check `winner`** in `gameOverPush()`:
   ```java
   if (winner != null) {
       // write <winner-id> and <winner-username>
   } else {
       // omit them or write empty elements to indicate draw
   }
   ```
2. **Fix `winner.toString()` → `winner.getUserId().toString()`** for the `<winner-id>` element.

---

## 17. `GameHandler.gameInviteResponse()` — Ignored `acceptGame()` Return Value

**Status:** `gameManager.acceptGame()` returns `Optional<Game>` — empty if one player is already in a game. The handler ignores this and proceeds as if the game was accepted, which can send incorrect pushes and create inconsistent state.

**Implementation:**

1. **Check the return value**:
   ```java
   Optional<Game> game = gameManager.acceptGame(gameId);
   if (game.isEmpty()) {
       // send ALREADY_IN_GAME error to the invitee
       return;
   }
   ```
2. Only send the acceptance push and game-start info if the game was successfully accepted.

---

## 18. Unit Tests for Game Logic

**Status:** Zero test coverage for `DotsAndBoxesGame`, `DotsAndBoxesMove`, `DotsAndBoxesMoveCodec`, `GameManager`, or `GameHandler`. Only `PlayerStatsTest` and `LeaderboardTest` test statistical aggregation.

**Implementation:**

1. **Create `DotsAndBoxesGameTest`** — test:
   - Valid horizontal/vertical moves accepted
   - Diagonal moves rejected
   - Duplicate lines rejected
   - Out-of-bounds moves rejected
   - Turn enforcement
   - Box capture detection (single and double)
   - Bonus turn on capture
   - Game over detection
   - Winner determination (p1 wins, p2 wins, draw)
2. **Create `DotsAndBoxesMoveTest`** — test coordinate normalization.
3. **Create `DotsAndBoxesMoveCodecTest`** — test encode/decode roundtrip.
4. **Create `GameManagerTest`** — test game lifecycle (create, accept, decline, end).
5. **Fix broken tests**: `StateMachineTest` and `OptionScreenTest` reference non-existent API methods (`registerScreen`, `transitionTo(String)`, etc.) — they must be rewritten for the current stack-based state machine.

---

## 19. Search Error / Empty Results Handling

**Status:** Search screens (`SearchForPlayerScreen`, `SearchInviteScreen`) only handle `SUCCESS`. On `ERROR`, nothing happens — the user sees no feedback. Empty result lists show only "back" with no "no results found" message.

**Implementation:**

1. **In `ClientSearchHandler.searchPlayers()`**, handle error responses and return an error indicator (e.g., `Optional.empty()` or a Result type).
2. **In search screens**, display an error message on failure and a "No players found" message on empty results.

---

## 20. Report / Deliverable Requirements

The assignment requires a report covering specific topics. These are documentation, not code, but should be tracked:

| # | Report Requirement | Notes |
|---|---|---|
| 1 | Architecture diagram (components + data flows) | Should show Client, Server, TCP framing, XML protocol, Persistence |
| 2 | Server description: concurrent vs iterative | Server is **concurrent** (thread-per-connection) |
| 3 | Client description: thin vs fat | Client is **fat** (local game state, optimistic move validation) |
| 4 | Transport protocol + ports | TCP on port 5555 (configurable). No UDP used. |
| 5 | Persistence data structures + XSD | XML persistence with `users.xsd` + `config.xsd`. Classes: `User`, `PlayerStats`, `MatchRecord` |
| 6 | Application protocol formalization | XML wire format with `protocol.xsd`. Syntax: XML elements. Semantics: per-action handler logic. Timing: request-response + server-push. |
| 7 | Screenshots of working application | Capture CLI screens for all major flows |
| 8 | Conclusion: advantages/disadvantages | Cover: extensibility, fault tolerance, security, transparency, concurrency |

---

## Priority Summary

| Priority | Item | Effort |
|----------|------|--------|
| **HIGH** | 4. Box ownership visualization | Medium |
| **HIGH** | 5. Bonus turn notification | Small |
| **HIGH** | 6. Draw/tie handling (bug + NPE) | Medium |
| **HIGH** | 7. Board size configurability | Medium |
| **HIGH** | 1. Registration with photo/nationality/dob | Medium |
| **HIGH** | 15. Connection.sendMessage() thread safety | Small |
| **HIGH** | 16. gameOverPush NPE fix | Small |
| **HIGH** | 17. acceptGame() return value check | Small |
| **MEDIUM** | 3. Leaderboard client access | Medium |
| **MEDIUM** | 8. Game forfeiture | Medium |
| **MEDIUM** | 10. Incremental persistence | Small |
| **MEDIUM** | 11. Real-time invite notification | Small |
| **MEDIUM** | 13. GAME_OVER push handling | Small |
| **MEDIUM** | 18. Game logic unit tests | Medium |
| **MEDIUM** | 2. Photo display | Small |
| **LOW** | 9. Disconnection/reconnection | Large |
| **LOW** | 12. Cancel pending invite | Medium |
| **LOW** | 14. Stale invite list cleanup | Small |
| **LOW** | 19. Search error handling | Small |
| **LOW** | 20. Report documentation | N/A |
