package se.ltu.eduo.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.user.model.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Integer> {

    UserCredential findUserByUsernameAndPassword(String username, String password);

    UserCredential findByUsername(String username);
}
