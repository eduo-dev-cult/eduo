package se.ltu.eduo.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.user.model.UserPreferences;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Integer> { }
