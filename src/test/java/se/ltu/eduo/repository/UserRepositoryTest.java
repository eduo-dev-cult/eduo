package se.ltu.eduo.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.TestContainersInitializer;
import se.ltu.eduo.model.User;
import se.ltu.eduo.model.UserCredential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the repository and database constraint layer in isolation.
 *
 * Uses @DataJpaTest, which loads only JPA-related beans (repositories,
 * EntityManager) — no service layer, no web layer. This keeps the tests
 * fast and focused on database behaviour.
 *
 * The "test" profile activates application-test.properties, which points
 * Spring at a Testcontainers-managed PostgreSQL instance instead of the
 * local dev database.
 */
@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(TestContainersInitializer.class)
@ContextConfiguration(initializers = TestContainersInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    // ---------------------------------------------------------------
    // Helper — builds a persisted User without going through AuthService
    // ---------------------------------------------------------------

    /**
     * Creates and saves a minimal valid User directly via the repository.
     * Tests in this class are about the data layer, not the service layer,
     * so bypassing AuthService is intentional here.
     */
    private User persistUser(String firstName, String lastName) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return userRepository.save(user);
    }

    /**
     * Creates and saves a UserCredential linked to the given User.
     */
    private UserCredential persistCredential(User user, String username, String password) {
        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setUsername(username);
        credential.setPassword(password);
        return credentialRepository.save(credential);
    }

    // ---------------------------------------------------------------
    // findUserByUsernameAndPassword
    // ---------------------------------------------------------------

    /**
     * A matching username and password should return the credential row.
     */
    @Test
    void findByUsernameAndPassword_returnsCredential_whenBothMatch() {
        User user = persistUser("Anna", "Larsson");
        persistCredential(user, "alarsson", "hunter2");

        UserCredential result = credentialRepository.findUserByUsernameAndPassword("alarsson", "hunter2");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("alarsson");
    }

    /**
     * A correct username paired with the wrong password should return null,
     * not throw an exception.
     */
    @Test
    void findByUsernameAndPassword_returnsNull_whenPasswordIsWrong() {
        User user = persistUser("Anna", "Larsson");
        persistCredential(user, "alarsson", "hunter2");

        UserCredential result = credentialRepository.findUserByUsernameAndPassword("alarsson", "wrongpassword");

        assertThat(result).isNull();
    }

    /**
     * A username that does not exist at all should return null.
     */
    @Test
    void findByUsernameAndPassword_returnsNull_whenUsernameUnknown() {
        UserCredential result = credentialRepository.findUserByUsernameAndPassword("nobody", "irrelevant");

        assertThat(result).isNull();
    }

    // ---------------------------------------------------------------
    // Unique username constraint
    // ---------------------------------------------------------------

    /**
     * Two users may not share a username. The database constraint should
     * reject the second insert.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void insertCredential_throwsException_whenUsernameIsDuplicate() {
        User first = persistUser("Anna", "Larsson");
        persistCredential(first, "dupename", "pass1");

        User second = persistUser("Anders", "Larsson");

        assertThatThrownBy(() -> persistCredential(second, "dupename", "pass2"))
                .isInstanceOf(Exception.class); // narrows to DataIntegrityViolationException at runtime
    }

    // ---------------------------------------------------------------
    // One-to-one enforcement on UserCredential
    // ---------------------------------------------------------------

    /**
     * A user should not be able to have two credential rows.
     * UserCredential uses user_id as its primary key, so a second insert
     * for the same user is a primary key violation.
     */
    @Test
    void insertCredential_throwsException_whenUserAlreadyHasCredential() {
        User user = persistUser("Anna", "Larsson");
        persistCredential(user, "alarsson", "pass1");

        assertThatThrownBy(() -> persistCredential(user, "alarsson2", "pass2"))
                .isInstanceOf(Exception.class);
    }

    // ---------------------------------------------------------------
    // Cascade delete
    // ---------------------------------------------------------------

    /**
     * Deleting a User should cascade and remove the associated
     * UserCredential row. Orphaned credential rows should not remain.
     */
    @Test
    void deleteUser_cascadesToCredential() {
        User user = persistUser("Anna", "Larsson");
        persistCredential(user, "alarsson", "hunter2");
        Integer userId = user.getId();

        userRepository.deleteById(userId);

        entityManager.flush();
        entityManager.clear();
        // needed because the whole method is transactional, cascade delete doesn't commit before
        // assertion is made

        assertThat(credentialRepository.findById(userId)).isEmpty();
    }
}