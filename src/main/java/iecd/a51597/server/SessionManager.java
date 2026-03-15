package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final long SESSION_TIMEOUT_SECONDS = 60 * 30; // 30 mins
    private static final Logger logger = LogManager.getLogger(SessionManager.class);

    // sessionId -> session
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    // userId -> sessionId
    private final Map<UUID, UUID> userSessionIndex = new ConcurrentHashMap<>();

    public Session createSession(User user) {
        UUID existingToken = userSessionIndex.get(user.getUserId());
        if (existingToken != null) {
            sessions.remove(existingToken);
            logger.info("Invalidated previous session for user {}", user.getUsername());
        }

        Session session = new Session(user);
        sessions.put(session.getToken(), session);
        userSessionIndex.put(user.getUserId(), session.getToken());

        logger.info("Session created for user {} [token={}]", user.getUsername(), session.getToken());
        return session;
    }

    public Optional<Session> validate(UUID token) {
        if (token == null) return Optional.empty();

        Session session = sessions.get(token);

        if (session == null) {
            return Optional.empty();
        }

        if (session.isExpired(SESSION_TIMEOUT_SECONDS)) {
            invalidate(token);
            logger.info("Session expired for user {}", session.getUsername());
            return Optional.empty();
        }

        session.refresh(); // sliding window — activity resets the clock
        return Optional.of(session);
    }

    public void invalidate(UUID token) {
        Session removed = sessions.remove(token);
        if (removed != null) {
            userSessionIndex.remove(removed.getUserId());
            logger.info("Session invalidated for user {}", removed.getUsername());
        }
    }

    public int activeSessionCount() {
        return sessions.size();
    }
}