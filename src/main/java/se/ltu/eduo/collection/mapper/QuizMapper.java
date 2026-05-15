package se.ltu.eduo.collection.mapper;

import org.mapstruct.*;
import se.ltu.eduo.collection.dto.QuizDto;
import se.ltu.eduo.collection.model.Quiz;

@Mapper(unmappedTargetPolicy = ReportingPolicy.WARN, componentModel = MappingConstants.ComponentModel.SPRING)
public interface QuizMapper {
    Quiz toEntity(QuizDto quizDto);

    QuizDto toDto(Quiz quiz);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Quiz partialUpdate(QuizDto quizDto, @MappingTarget Quiz quiz);
}