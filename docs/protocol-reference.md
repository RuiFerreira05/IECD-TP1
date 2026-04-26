# IECD-TP1 | XML Protocol Reference

This document defines the structured XML protocol used for communication between the Client and Server in the Dots and Boxes application.

## 1. Message Structure

Every protocol message is wrapped in a `<message>` envelope.

```xml
<message type="REQUEST|RESPONSE|PUSH" id="uuid" version="1.0">
    <header>
        <action>ACTION_NAME</action>
        <session>SESSION_TOKEN</session>   <!-- Omit if not authenticated -->
        <timestamp>ISO-8601</timestamp>
    </header>
    <body>
        <!-- Action-specific payload -->
    </body>
</message>
```

| Component | Description |
| :--- | :--- |
| `type` | `REQUEST` (Client → Server), `RESPONSE` (Server → Client reply), or `PUSH` (Server-initiated). |
| `id` | A unique UUID. Responses must echo the request's ID. Pushes generate a fresh ID. |
| `version` | Protocol version (current: `1.0`). |
| `action` | The semantic action type (e.g., `LOGIN`, `GAME_MOVE`). |
| `session` | UUID session token provided after successful login. |

---

## 2. Authentication

### Register
**Action:** `REGISTER`

| Message | Payload Description |
| :--- | :--- |
| **Request** | `<username>`, `<password>` (plaintext) |
| **Response (OK)** | `<status>OK</status>` |
| **Response (Err)** | `<status>ERROR</status>` with `<error code="USERNAME_TAKEN">` |

### Login
**Action:** `LOGIN`

| Message | Payload Description |
| :--- | :--- |
| **Request** | `<username>`, `<password>` |
| **Response (OK)** | `<status>OK</status>`, `<session>UUID</session>`, and full `<user>` object (ID, username, photo, nationality, dob, stats). |
| **Response (Err)** | `<status>ERROR</status>` with `<error code="AUTH_FAILED">` |

### Logout
**Action:** `LOGOUT` (Requires Session)

| Message | Payload Description |
| :--- | :--- |
| **Request** | Empty `<body>` |
| **Response (OK)** | `<status>OK</status>` |

---

## 3. Profile Management
*Requires an active session.*

### Update Profile
**Action:** `UPDATE_PROFILE`

Supports partial updates. Any field omitted remains unchanged.

**Request Payload:**
```xml
<body>
    <username>new_username</username>    <!-- Optional -->
    <password>new_password</password>    <!-- Optional -->
    <photo>base64_encoded_bytes</photo>  <!-- Optional -->
    <nationality>PT</nationality>       <!-- Optional -->
    <dob>2005-04-11</dob>               <!-- Optional -->
</body>
```

**Response:** Returns `<status>OK</status>` and the updated `<user>` object on success.

---

## 4. User Search

### Search Users
**Action:** `SEARCH_USERS`

| Message | Payload Description |
| :--- | :--- |
| **Request** | `<query>` (substring search, case-insensitive) |
| **Response** | `<status>OK</status>` and a `<results>` list containing `<user>` objects. |

---

## 5. Game Lifecycle

### Game Invite
**Action:** `GAME_INVITE`

*   **Request (Inviter):** `<target-user-id>UUID</target-user-id>`
*   **Response (Inviter):** `<status>OK</status>`, `<game-id>UUID</game-id>`
*   **Push (Invitee):** `<from-user-id>`, `<from-username>`, `<game-id>`

### Cancel Invite
**Action:** `GAME_INVITE_CANCEL`

*   **Request (Inviter):** `<game-id>UUID</game-id>`
*   **Push (Invitee):** `<game-id>UUID</game-id>` (Notification that the invite was withdrawn)

### Invite Response
**Action:** `GAME_INVITE_RESPONSE`

*   **Request (Invitee):** `<game-id>`, `<accept>true|false</accept>`
*   **Push (Inviter):** `<game-id>`, `<accepted>true|false</accepted>`, `<opponent-username>`

### Game Move
**Action:** `GAME_MOVE`

**Payload:**
```xml
<body>
    <game-id>UUID</game-id>
    <move><![CDATA[x1,y1,x2,y2]]></move>
</body>
```

*   **Response:** `<status>OK</status>` if valid, or `ERROR` with `INVALID_MOVE`.
*   **Push:** Mirrored move sent to the opponent.

### Surrender
**Action:** `SURRENDER`

*   **Request:** `<game-id>UUID</game-id>`
*   **Response:** `<status>OK</status>`

---

## 6. Game Termination

### Game Over
**Action:** `GAME_OVER` (Push Only)

Sent when a player wins or surrenders.

```xml
<body>
    <game-id>UUID</game-id>
    <winner-id>UUID</winner-id>
    <winner-username>name</winner-username>
    <reason>SURRENDER</reason> <!-- Optional -->
    <user><!-- Updated profile of the receiver --></user>
</body>
```

### Game Draw
**Action:** `GAME_OVER_DRAW` (Push Only)

Sent when the board is full and scores are equal. Returns the updated `<user>` profile.

---

## 7. Error Codes

| Code | Description |
| :--- | :--- |
| `AUTH_FAILED` | Invalid username or password. |
| `USERNAME_TAKEN` | Attempted to register/rename to an existing username. |
| `SESSION_EXPIRED` | Token is invalid or has timed out. |
| `NOT_AUTHENTICATED` | Session token missing from header. |
| `USER_NOT_ONLINE` | Target user for invitation is not connected. |
| `ALREADY_IN_GAME` | User is already participating in an active match. |
| `INVALID_MOVE` | Move rejected by game rules (e.g., out of turn). |
| `MALFORMED_REQUEST` | XML parsing failed or schema validation error. |
| `OUTDATED_PROTOCOL` | Version mismatch between Client and Server. |

---

## 8. Special Values

*   **Error No ID:** `00000000-0000-0000-0000-000000000000`
    *   Used in a `RESPONSE` if the server fails to parse the original `REQUEST` ID.
*   **Session Timeout:** Sessions are invalidated after 30 minutes of inactivity.
