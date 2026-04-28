package se.ltu.eduo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.model.User;
import se.ltu.eduo.model.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Integer> {

    UserCredential findUserByUsernameAndPassword(String username, String password);

    UserCredential findByUsername(String username);
}
