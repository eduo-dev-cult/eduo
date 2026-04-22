package se.ltu.eduo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.ltu.eduo.SessionContext;
import se.ltu.eduo.model.User;
import se.ltu.eduo.model.UserCredential;
import se.ltu.eduo.repository.UserCredentialRepository;
import se.ltu.eduo.repository.UserRepository;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private SessionContext sessionContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    public User createUser(String firstName, String lastName, String username, String password) {
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
     * log in user by setting them as the active user in sessioncontext
     * @param username
     * @param password
     * @return true on successful login
     */
    public boolean LogInUser(String username, String password)
    {
        Integer userID = credentialRepository.findUserByUsernameAndPassword(username, password).getId();

        Optional<User> user = userRepository.findById(userID);

        //fixme fails on null user, good behaviour for test
        sessionContext.setCurrentUser(user.get());

        return true;
    }

}
