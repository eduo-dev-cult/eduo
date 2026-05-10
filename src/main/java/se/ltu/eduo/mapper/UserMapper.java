package se.ltu.eduo.mapper;

import org.mapstruct.*;
import se.ltu.eduo.dto.UserDto;
import se.ltu.eduo.model.User;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    User toEntity(UserDto userDto);

    UserDto toDto(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User partialUpdate(UserDto userDto, @MappingTarget User user);
}