package se.ltu.eduo.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.user.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {}
