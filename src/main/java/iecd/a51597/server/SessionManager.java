package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final long SESSION_TIMEOUT_SECONDS = 60 * 30; // 30 mins
    private static final Logger logger = LogManager.getLogger(SessionManager.class);

    // UUID -> Session
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // userId -> UUID
    private final Map<Integer, String> userSessionIndex = new ConcurrentHashMap<>();

    public Session createSession(int userId, String username) {
        String existingToken = userSessionIndex.get(userId);
        if (existingToken != null) {
            sessions.remove(existingToken);
            logger.info("Invalidated previous session for user {}", userId);
        }

        Session session = new Session(userId, username);
        sessions.put(session.getToken(), session);
        userSessionIndex.put(userId, session.getToken());

        logger.info("Session created for user {} [token={}]", username, session.getToken());
        return session;
    }

    public Optional<Session> validate(String token) {
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

    public void invalidate(String token) {
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