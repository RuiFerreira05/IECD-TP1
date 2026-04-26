# Project Map: Dots and Boxes Server/Client

Welcome to the Dots and Boxes Server/Client project! This guide provides a comprehensive overview of the application architecture, its features, quirks, design decisions, and testing gotchas to help you onboard and navigate the codebase efficiently.

## 1. High-Level Overview

This is a **Client-Server CLI Game** written in Java 25. The project has a single Maven module, compiled using `mvn compile`. 
The application allows users to register, log in, manage their profiles, search for players, issue game invites, and play matches of **Dots and Boxes** in a terminal.
It consists of two main singletons representing entry points:
- **Server (`iecd.a51597.server.Server`)**: Manages networking, authentication, routing protocol requests, user persistence, and server-authoritative game state validations.
- **Client (`iecd.a51597.client.Client`)**: Manages local session state, connection to the server, and a state-machine driven text-based user interface.

## 2. Architecture & Top-Level Modules

The codebase is organized into three primary modules within `iecd.a51597`:

- **`common`**: Contains shared DTOs, the XML messaging protocol, and abstract game logic (interfaces like `Game`, `MoveCodec`, and the Dots and Boxes specific implementation `DotsAndBoxesGame`).
- **`server`**: Everything needed for running the background server logic. Handles TCP connections, dispatches XML frames, parses messages, and persists user state.
- **`client`**: The CLI-driven user interface. Maintains a connection instance and uses a `StateMachine` stack to render various text menus and the active game board.

---

## 3. Package Deep-Dive

### Server (`iecd.a51597.server`)
The core `Server.java` is a singleton that composes multiple manager objects and wires up the routing system.

- **`server.network`**:
  - `ListenerThread`: Spawns standard Java `Socket` listeners on the configured port.
  - `Connection`: Manages streams to individual clients, reading frames (4-byte length prefix + XML payload).
- **`server.handlers`**:
  - `MessageDispatcher`: The central router. Parses incoming `byte[]` arrays using `XMLParser` and dispatches `ActionType` commands (e.g., `LOGIN`, `GAME_INVITE`) to their specific handlers (`AuthHandler`, `ProfileHandler`, `SearchHandler`, `GameHandler`).
- **`server.store` & `server.persistence`**:
  - `UserStore`: An in-memory concurrent map repository. It maintains dual indexes mapping `UUID -> User` and `Username -> User`.
  - `PersistenceManager`: Spawns a background thread that periodically persists `UserStore` state into an XML file using `UserRepository` (usually the `XmlUserRepository` implementation).
  - *Quirk/Decision*: The `UserStore.hash()` method is `package-private` rather than `private`. This was an intentional choice to facilitate specific package-scoped tests that interact with user passwords.
- **`server.session`**:
  - `SessionManager`: Manages active authentication tokens. Used heavily by handlers to verify session legitimacy prior to accepting game invites or moves.
- **`server.game`**:
  - `GameManager`: Keeps track of active games and pending invites.

### Client (`iecd.a51597.client`)
The `Client.java` is the client-side singleton that bootstraps configurations and sets up the server connection stream.

- **`client.cli` & `client.cli.screens`**:
  - `ClientCliHandler`: Kicks off the standard input polling loop.
  - `StateMachine`: Manages a stack-based navigation hierarchy using `Screen` objects. By pushing/popping screens, the user traverses from the `LoginScreen` or `RegisterScreen` down to the `MainMenuScreen` and `GameScreen`.
- **`client.game`**:
  - `GameController`: Orchestrates live gameplay. It contains a local copy of the `DotsAndBoxesGame` state.
  - *Decision*: When the local user attempts a move, the controller first verifies validity *locally* using `localGameState.applyMove()`. If the move is locally valid, it translates it to a `GAME_MOVE` request and pushes it to the server for authoritative validation. Remote moves coming from the server are passed into `applyOpponentMove()`.
- **`client.session`**:
  - `ClientSessionManager`: Safely stores the local user's metadata (UUID, stats) and active session token after a successful login.

### Common (`iecd.a51597.common`)

- **`common.protocol`**:
  - `XMLParser`: Very important class. Validates incoming XML using `schemas/protocol.xsd` and produces strongly-typed `Message` and `MessageBody` objects.
  - *Quirk*: Not all properties are required. For example, `UserDTO` fields like `nationality` and `dob` are nullable, which translates directly to omissions in the XML strings (the tags are completely left out, rather than being empty).
- **`common.game.dotsandboxes`**:
  - `DotsAndBoxesGame`: Contains the core logic for the board state. Because it lives in `common`, both the server (for authoritative validation) and client (for immediate UI feedback) leverage the exact same rule engine.

---

## 4. Key Patterns & Decisions

- **Singleton Usage**: The root `Server` and `Client` operate as thread-safe singletons accessible via `getInstance()`. This provides a centralized context reference for nested handlers.
- **Protocol Framing**: Sockets are stream-oriented, so to distinguish distinct messages, every message is prepended with a 4-byte big-endian integer denoting the exact length of the trailing XML payload.
- **XSD Validation**: Message schemas, user persistence files, and system configs all use XML. However, they use *three separate XSD schema validations*. Do not assume a uniform schema across different domains.
- **Fail-Fast Error Handling**: Protocol messages use an `ErrorCodeType` enum to explicitly fail-fast when a request is malformed, ensuring clients get explicit feedback strings in the UI.

## 5. Configuration & Persistence

- Config and persistence files are **not** bundled into the final `.jar` resources. They are loaded strictly from the Current Working Directory (CWD) on startup.
- **`config/config.xml`**: Defines server parameters (port, loop timings).
- **`config/client_config.xml`**: Defines client settings (target IP/port).
- **`data/users.xml`**: The persisted repository state managed by the `PersistenceManager` background thread. Note: `data/` and `logs/` are `.gitignore`d, ensuring no developer leaks local data!

## 6. Testing Gotchas

- **Mutable Statics**: Several classes, like `ServerConfiguration`, use mutable static variables. Unit tests in `PersistenceManagerTest` and `XMLServerMessageBuilderTest` actively modify them. To prevent inter-test flakiness, ensure you utilize `@BeforeEach` or `@AfterEach` hooks to restore static variables.
- **Default Package Tests**: Interestingly, all JUnit 5 tests reside in `src/test/java/` under the **default package** (no `package` declaration), despite utilizing imports to `iecd.a51597.*`. 
- **User Optional fields**: `User` objects have nullable `nationality` and `dob`. `XMLServerMessageBuilder` excludes null fields entirely rather than emitting empty tags. Write specific tests that check payloads with missing fields vs hydrated fields. 
- **Dependencies**: The project limits assertions strictly to JUnit 5 built-ins (no Hamcrest, no AssertJ). Mockito is available for mocking external dependencies like connections or parsers.