package se.ltu.eduo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.model.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {}
