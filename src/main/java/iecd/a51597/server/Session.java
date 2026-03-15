package iecd.a51597.server;

import java.time.Instant;
import java.util.UUID;

public class Session {

    private final UUID token;
    private final UUID userId;
    private final String username;
    private volatile Instant lastActivity;

    public Session(User user) {
        this.token = UUID.randomUUID();
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.lastActivity = Instant.now();
    }

    public boolean isExpired(long timeoutSeconds) {
        return Instant.now().isAfter(lastActivity.plusSeconds(timeoutSeconds));
    }

    public void refresh() {
        this.lastActivity = Instant.now();
    }

    public UUID getToken()    { return token; }
    public UUID getUserId()      { return userId; }
    public String getUsername() { return username; }
    public Instant getLastActivity() { return lastActivity; }
}