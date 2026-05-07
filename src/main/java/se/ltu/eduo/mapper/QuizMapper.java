package se.ltu.eduo.mapper;

import org.mapstruct.*;
import se.ltu.eduo.dto.QuizDto;
import se.ltu.eduo.model.project.Quiz;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface QuizMapper {
    Quiz toEntity(QuizDto quizDto);

    QuizDto toDto(Quiz quiz);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Quiz partialUpdate(QuizDto quizDto, @MappingTarget Quiz quiz);
}