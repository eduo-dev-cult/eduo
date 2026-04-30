package se.ltu.eduo.mapper;

import org.mapstruct.*;
import se.ltu.eduo.dto.GenerationDto;
import se.ltu.eduo.model.project.Generation;
import se.ltu.eduo.model.project.Quiz;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface GenerationMapper {
    Generation toEntity(GenerationDto generationDto);

    @AfterMapping
    default void linkQuiz(@MappingTarget Generation generation) {
        Quiz quiz = generation.getQuiz();
        if (quiz != null) {
            quiz.setGeneration(generation);
        }
    }

    GenerationDto toDto(Generation generation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Generation partialUpdate(GenerationDto generationDto, @MappingTarget Generation generation);
}