package iecd.a51597.server.persistence;

import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.store.UserStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles XML persistence of server state (currently user repository).
 */
public class PersistenceManager {

    private static final Logger logger = LogManager.getLogger(PersistenceManager.class);
    private final UserRepository userRepository;

    private final UserStore userStore;

    /**
     * Creates a persistence manager for a user store.
     *
     * @param userStore target user store
     */
    public PersistenceManager(UserStore userStore) {
        this.userStore = userStore;
        this.userRepository = RepositoryFactory.createUserRepository(ServerConfiguration.PERSISTENCE_TYPE, logger);
    }

    /**
     * Loads all persisted data into memory.
     */
    public void load() {
        userRepository.loadInto(userStore);
    }

    /**
     * Saves in-memory state to persistence files.
     */
    public void save() {
        userRepository.saveFrom(userStore);
    }
}