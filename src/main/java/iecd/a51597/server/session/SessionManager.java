package iecd.a51597.server.session;

import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.network.Connection;
import iecd.a51597.server.store.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Logger logger = LogManager.getLogger(SessionManager.class);

    // sessionToken -> Session
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    // userId -> Session
    private final Map<UUID, Session> userSessions = new ConcurrentHashMap<>();

    public Session createSession(User user, Connection connection) {
        Session existing = userSessions.get(user.getUserId());
        if (existing != null) {
            sessions.remove(existing.getToken());
            userSessions.remove(user.getUserId());
            logger.info("Invalidated previous session for user {}", user.getUsername());
        }

        Session session = new Session(user, connection);
        sessions.put(session.getToken(), session);
        userSessions.put(user.getUserId(), session);

        logger.info("Session created for user {} [token={}]", user.getUsername(), session.getToken());
        return session;
    }

    public Optional<Session> validate(UUID token) {
        if (token == null) return Optional.empty();

        Session session = sessions.get(token);
        if (session == null) return Optional.empty();

        if (session.isExpired(ServerConfiguration.SESSION_TIMEOUT_SECONDS)) {
            invalidate(token);
            logger.info("Session expired for user {}", session.getUser().getUsername());
            return Optional.empty();
        }

        session.refresh();
        return Optional.of(session);
    }

    public void invalidate(UUID token) {
        Session removed = sessions.remove(token);
        if (removed != null) {
            userSessions.remove(removed.getUserId());
            logger.info("Session invalidated for user {}", removed.getUser().getUsername());
        }
    }

    public void invalidateByUserId(UUID userId) {
        Session session = userSessions.get(userId);
        if (session != null) {
            invalidate(session.getToken());
        }
    }

    public Optional<Session> getSessionByUserId(UUID userId) {
        return Optional.ofNullable(userSessions.get(userId));
    }

    public int activeSessionCount() {
        return sessions.size();
    }
}