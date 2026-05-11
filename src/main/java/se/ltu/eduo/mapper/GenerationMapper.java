package se.ltu.eduo.mapper;

import org.mapstruct.*;
import se.ltu.eduo.dto.CollectionDto;
import se.ltu.eduo.dto.GenerationDto;
import se.ltu.eduo.dto.request.CreateGenerationRequest;
import se.ltu.eduo.model.collection.Generation;
import se.ltu.eduo.model.collection.GenerationSourceMaterial;
import se.ltu.eduo.model.collection.Quiz;

@Mapper(unmappedTargetPolicy = ReportingPolicy.WARN, componentModel = MappingConstants.ComponentModel.SPRING)
public interface GenerationMapper {
    @Mapping(target = "sourceMaterials", ignore = true)
    Generation toEntity(GenerationDto generationDto);

    //för mapping av CreateGenerationRequest till Generation i createGeneration
    @Mapping(target = "collection", ignore = true)
    @Mapping(target = "sourceMaterials", ignore = true)
    Generation toEntity(CreateGenerationRequest request);

    @AfterMapping
    default void linkSourceMaterials(@MappingTarget Generation generation)
    {
        generation.getSourceMaterials()
                  .forEach(sourceMaterial -> sourceMaterial.setGeneration(generation));
    }

    @AfterMapping
    default void linkQuiz(@MappingTarget Generation generation)
    {
        Quiz quiz = generation.getQuiz();
        if (quiz != null)
        {
            quiz.setGeneration(generation);
        }
    }

    @Mapping(target = "collectionId", source = "collection.id")
    GenerationDto toDto(Generation generation);

    @Mapping(target = "id", source = "sourceMaterial.id")
    @Mapping(target = "filename", source = "sourceMaterial.filename")
    @Mapping(target = "fileType", source = "sourceMaterial.fileType")
    @Mapping(target = "fileSizeBytes", source = "sourceMaterial.fileSizeBytes")
    @Mapping(target = "uploadedAt", source = "sourceMaterial.uploadedAt")
    CollectionDto.SourceMaterialDto toDto(GenerationSourceMaterial gsm);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "sourceMaterials", ignore = true)
    Generation partialUpdate(GenerationDto generationDto, @MappingTarget Generation generation);
}