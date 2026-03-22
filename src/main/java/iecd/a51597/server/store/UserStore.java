package iecd.a51597.server.store;

import iecd.a51597.server.User;
import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserStore {

    // UUID -> User
    ConcurrentHashMap<UUID, User> userStore = new ConcurrentHashMap<>();

    //Username -> User
    ConcurrentHashMap<String, User> usernameIndex = new ConcurrentHashMap<>();

    // CREATION
    User register(String username, String passwordHash) throws UsernameAlreadyTakenException {
        return null; //TODO
    }

    // LOOKUP
    Optional<User> findByCredentials(String username, String passwordHash) {
        return null; //TODO
    }
    Optional<User> findById(UUID userId) {
        return null; //TODO
    }
    Optional<User> findByUsername(String username) {
        return null; //TODO
    }
    List<User> searchByUsername(String query) {
        return null; //TODO
    }

    // UPDATE
    void updateUsername(UUID userId, String newUsername) throws UsernameAlreadyTakenException {
        //TODO
    }
    void updatePassword(UUID userId, String newPasswordHash) {
        //TODO
    }
    void updatePhoto(UUID userId, String photo) {
        //TODO
    }

    // DELETION
    void delete(UUID userId) {
        //TODO
    }

    // PERSISTENCE
    void loadUser(User user) {
        //TODO
    }
    Collection<User> getAllUsers() {
        return null; //TODO
    }
}
