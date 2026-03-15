package iecd.a51597.server;

import java.time.Instant;
import java.util.UUID;

public class Session {

    private final String token;
    private final int userId;
    private final String username;
    private volatile Instant lastActivity;

    public Session(int userId, String username) {
        this.token = UUID.randomUUID().toString();  // the session token IS a UUID
        this.userId = userId;
        this.username = username;
        this.lastActivity = Instant.now();
    }

    public boolean isExpired(long timeoutSeconds) {
        return Instant.now().isAfter(lastActivity.plusSeconds(timeoutSeconds));
    }

    public void refresh() {
        this.lastActivity = Instant.now();
    }

    public String getToken()    { return token; }
    public int getUserId()      { return userId; }
    public String getUsername() { return username; }
    public Instant getLastActivity() { return lastActivity; }
}