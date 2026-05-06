package se.ltu.eduo.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.TestContainersInitializer;
import se.ltu.eduo.model.User;
import se.ltu.eduo.repository.UserCredentialRepository;
import se.ltu.eduo.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests AuthService behaviour end-to-end against a real PostgreSQL instance
 * managed by Testcontainers.
 *
 * Uses @SpringBootTest rather than @DataJpaTest because AuthService is a
 * Spring-managed bean that requires the full application context to be wired.
 *
 * Each test is @Transactional so that database changes are rolled back
 * automatically after each test, keeping tests independent of each other
 * without needing manual cleanup.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AuthServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    // ---------------------------------------------------------------
    // createUser
    // ---------------------------------------------------------------

    /**
     * A newly created user should be persisted and have a generated ID.
     */
    @Test
    void createUser_persistsUserWithGeneratedId() {
        User user = authService.createUser("Anna", "Larsson", "alarsson", "hunter2");

        assertThat(user.getId()).isNotNull();
        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    /**
     * Creating a user should also persist a linked UserCredential row.
     */
    @Test
    void createUser_persistsLinkedCredential() {
        User user = authService.createUser("Anna", "Larsson", "alarsson", "hunter2");

        assertThat(credentialRepository.findById(user.getId())).isPresent();
    }

    // ---------------------------------------------------------------
    // loginUser — success path
    // ---------------------------------------------------------------

    /**
     * Valid credentials should return a non-empty Optional containing
     * the correct User.
     */
    @Test
    void loginUser_returnsUser_whenCredentialsAreValid() {
        authService.createUser("Anna", "Larsson", "alarsson", "hunter2");

        Optional<User> result = authService.logInUser("alarsson", "hunter2");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Anna");
    }

    /**
     * A successful login should update the lastLoginAt timestamp on the
     * credential row. This verifies the side-effect, not just the return value.
     */
    @Test
    void loginUser_updatesLastLoginAt_onSuccess() {
        User user = authService.createUser("Anna", "Larsson", "alarsson", "hunter2");

        authService.logInUser("alarsson", "hunter2");

        assertThat(credentialRepository.findById(user.getId()))
                .isPresent()
                .get()
                .extracting(c -> c.getLastLoginAt())
                .isNotNull();
    }

    // ---------------------------------------------------------------
    // loginUser — failure paths
    // ---------------------------------------------------------------
    /**
     * A correct username paired with the wrong password should return
     * an empty Optional, not throw an exception.
     */
    @Test
    void loginUser_returnsEmpty_whenPasswordIsWrong() {
        authService.createUser("Anna", "Larsson", "alarsson", "hunter2");

        Optional<User> result = authService.logInUser("alarsson", "wrongpassword");

        assertThat(result).isEmpty();
    }

    /**
     * An unknown username should return an empty Optional.
     * Same null-check fix applies as above.
     */
    @Test
    void loginUser_returnsEmpty_whenUsernameIsUnknown() {
        Optional<User> result = authService.logInUser("nobody", "irrelevant");

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // deleteUser
    // ---------------------------------------------------------------

    /**
     * Deleting a user by ID should remove the User row.
     */
    @Test
    void deleteUser_removesUserRow() {
        User user = authService.createUser("Anna", "Larsson", "alarsson", "hunter2");
        Integer userId = user.getId();

        authService.deleteUser(userId);

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    /**
     * Deleting a user should cascade and remove the linked credential row.
     */
    @Test
    void deleteUser_cascadesToCredential() {
        User user = authService.createUser("Anna", "Larsson", "alarsson", "hunter2");
        Integer userId = user.getId();

        authService.deleteUser(userId);

        entityManager.flush();
        entityManager.clear();
        // needed because the whole method is transactional, cascade delete doesn't commit before
        // assertion is made

        assertThat(credentialRepository.findById(userId)).isEmpty();
    }

    /**
     * Deleting a non-existent user ID should not throw an exception.
     * The current implementation uses deleteById which fails silently —
     * this test documents and protects that behaviour.
     */
    @Test
    void deleteUser_doesNotThrow_whenUserDoesNotExist() {
        authService.deleteUser(Integer.MAX_VALUE);
        // no assertion needed — the test passes if no exception is thrown
    }
}