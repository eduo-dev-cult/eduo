package se.ltu.eduo.user.mapper;

import org.mapstruct.*;
import se.ltu.eduo.user.model.UserPreferences;
import se.ltu.eduo.user.dto.UserPreferencesDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserPreferencesMapper {

    UserPreferencesDto toDto(UserPreferences userPreferences);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(UserPreferencesDto userPreferencesDto,
                       @MappingTarget UserPreferences userPreferences);
}
