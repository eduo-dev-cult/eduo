package se.ltu.eduo.collection.mapper;

import org.mapstruct.*;
import se.ltu.eduo.collection.dto.QuizDto;
import se.ltu.eduo.collection.model.Quiz;

@Mapper(unmappedTargetPolicy = ReportingPolicy.WARN, componentModel = MappingConstants.ComponentModel.SPRING)
public interface QuizMapper {
    // tell MapStruct to ignore these fields when mapping from DTO to entity, 
    // because they are not provided by the client but set by the service layer (
    // or in the case of Quiz.generation, set by @AfterMapping linkQuiz below).
    // "generation" is a back-reference to the owning Generation; it is set by
    // GenerationMapper's @AfterMapping linkQuiz, not from DTO data.
    @Mapping(target = "generation", ignore = true)
    Quiz toEntity(QuizDto quizDto);

    QuizDto toDto(Quiz quiz);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "generation", ignore = true) // same rationale as toEntity
    Quiz partialUpdate(QuizDto quizDto, @MappingTarget Quiz quiz);
}