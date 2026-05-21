package se.ltu.eduo.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import se.ltu.eduo.collection.model.GenerationFocusArea;
import se.ltu.eduo.collection.model.GenerationLanguage;
import se.ltu.eduo.collection.service.CollectionService;
import se.ltu.eduo.exception.UsernameAlreadyExistsException;
import se.ltu.eduo.user.model.User;
import se.ltu.eduo.user.model.UserCredential;
import se.ltu.eduo.user.model.UserPreferences;
import se.ltu.eduo.user.repository.UserCredentialRepository;
import se.ltu.eduo.user.repository.UserPreferencesRepository;
import se.ltu.eduo.user.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final CollectionService collectionService;
    private final UserPreferencesRepository userPreferencesRepository;

    /**
     * Creates a new user, associated credentials, and a default collection.
     *
     * @param firstName user's first name
     * @param lastName  user's last name
     * @param username  login username (max 8 characters)
     * @param password  login password
     * @return the newly created {@link User}
     */
    @Transactional
    public User createUser(
            String firstName,
            String lastName,
            String username,
            String password
    ) throws UsernameAlreadyExistsException {

        // Check that the username is not already taken.
        if (credentialRepository.findByUsername(username) != null) {
            throw new UsernameAlreadyExistsException(username);
        }

        // Create and save the user first, so it gets a database ID.
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);

        User savedUser = userRepository.save(user);

        // Create login credentials for the saved user.
        UserCredential credential = new UserCredential();
        credential.setUser(savedUser);
        credential.setUsername(username);
        credential.setPassword(password);

        credentialRepository.save(credential);

        // sets defaultUserPreferences when a user is created
        UserPreferences prefs = getUserPreferences(savedUser);

        userPreferencesRepository.save(prefs);

        // Every user should always have at least one collection available.
        collectionService.createCollection(
                savedUser.getId(),
                "My Collection",
                ""
        );

        return savedUser;
    }

    // method to set default userPreferences
    private static @NonNull UserPreferences getUserPreferences(User savedUser) {
        UserPreferences prefs = new UserPreferences();
        prefs.setUser(savedUser);

        prefs.setNumOfQuestions(10);
        prefs.setLanguage(GenerationLanguage.ENGLISH);
        prefs.setFocusArea(GenerationFocusArea.ENTIRE_MATERIAL);
        prefs.setTopics(null);

        prefs.setEasy(true);
        prefs.setMedium(false);
        prefs.setHard(false);

        prefs.setMultipleChoice(true);
        prefs.setOpenEnded(false);
        prefs.setTrueFalse(false);

        prefs.setQuestions(true);
        prefs.setCorrectAnswers(false);
        prefs.setExplanations(false);
        prefs.setDescription(false);
        return prefs;
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
    public Optional<User> logInUser(String username, String password) {
        UserCredential credential = credentialRepository.findUserByUsernameAndPassword(username, password);

        if (credential == null) {
            return Optional.empty();
        }

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
    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
        // Fails silently if id does not exist - might be fine?
    }
}