package se.ltu.eduo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.ltu.eduo.model.User;
import se.ltu.eduo.model.UserCredential;
import se.ltu.eduo.repository.UserCredentialRepository;
import se.ltu.eduo.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;

    /**
     * Creates a new user and associated credentials.
     *
     * @param firstName user's first name
     * @param lastName  user's last name
     * @param username  login username (max 8 characters)
     * @param password  login password
     * @return the newly created {@link User}
     */
    @Transactional
    public User createUser(String firstName, String lastName, String username, String password) {
        //check uniqueness
        if (credentialRepository.findByUsername(username) != null) throw new IllegalArgumentException("Username already exists");

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setUsername(username);
        credential.setPassword(password);
        credentialRepository.save(credential);

        return user;
    }

    /**
     * Authenticates a user and returns the User entity.
     * Updates {@code lastLoginAt} on success.
     *
     * @param username login username
     * @param password login password
     * @return Optional with user on successful login, empty Optional if credentials are invalid
     */
    @Transactional
    public Optional<User> LogInUser(String username, String password) {
        UserCredential credential = credentialRepository.findUserByUsernameAndPassword(username, password);
        if (credential == null) return Optional.empty();
        Optional<User> user = userRepository.findById(credential.getUser().getId());

        if (user.isPresent()) {
            credential.setLastLoginAt(Instant.now());
            credentialRepository.save(credential);
        }

        return user;
    }

    /**
     * Deletes a user and their associated credentials by ID.
     * Credential and preference rows are removed via database cascade.
     *
     * @param userId ID of the user to delete
     */
    @Transactional
    public void DeleteUser(Integer userId) {
        userRepository.deleteById(userId);
        //fails silently if id does not exist - might be fine?
    }

}