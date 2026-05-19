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


    // updates preferences by userId
    @Transactional
    public void updatePreferencesByUserId(Integer userId, UserPreferencesDto userPreferencesDto) {

        UserPreferences userPreferences = repository.findById(userId)
                .orElseThrow(() ->
                        new UserPreferencesNotFoundException(userId));

        mapper.updateFromDto(userPreferencesDto, userPreferences);

        repository.save(userPreferences);
    }
}
