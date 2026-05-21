package se.ltu.eduo.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import se.ltu.eduo.exception.UserPreferencesNotFoundException;
import se.ltu.eduo.user.dto.UserPreferencesDto;
import se.ltu.eduo.user.mapper.UserPreferencesMapper;
import se.ltu.eduo.user.model.UserPreferences;
import se.ltu.eduo.user.repository.UserPreferencesRepository;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository repository;
    private final UserPreferencesMapper mapper;

    // gets preferences by userId
    public UserPreferencesDto getPreferencesByUserId(Integer userId) {

        UserPreferences userPreferences = repository.findById(userId)
                .orElseThrow(() ->
                        new UserPreferencesNotFoundException(userId));

        return mapper.toDto(userPreferences);
    }

    // updates a preferences for a user
    @Transactional
    public UserPreferencesDto updatePreferencesByUserId(Integer userId, UserPreferencesDto dto) {

        UserPreferences userPreferences = repository.findById(userId)
                .orElseThrow(() -> new UserPreferencesNotFoundException(userId));

        mapper.updateFromDto(dto, userPreferences);

        UserPreferences saved = repository.save(userPreferences);

        return mapper.toDto(saved);
    }
}
