package se.ltu.eduo.mapper;

import org.mapstruct.*;
import se.ltu.eduo.dto.CollectionDto;
import se.ltu.eduo.dto.GenerationDto;
import se.ltu.eduo.model.collection.Collection;
import se.ltu.eduo.model.collection.Generation;
import se.ltu.eduo.model.collection.GenerationSourceMaterial;
import se.ltu.eduo.model.collection.SourceMaterial;

@Mapper(unmappedTargetPolicy = ReportingPolicy.WARN, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CollectionMapper {

    @Mapping(target = "owner", ignore = true)
    Collection toEntity(CollectionDto collectionDto);

    @AfterMapping
    default void linkSourceMaterials(@MappingTarget Collection collection)
    {
        collection.getSourceMaterials()
                  .forEach(sourceMaterial -> sourceMaterial.setCollection(collection));
    }

    @AfterMapping
    default void linkGenerations(@MappingTarget Collection collection)
    {
        collection.getGenerations()
                  .forEach(generation -> generation.setCollection(collection));
    }

    @Mapping(target = "ownerId", source = "owner.id")
    CollectionDto toDto(Collection collection);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "owner", ignore = true)
    Collection partialUpdate(CollectionDto collectionDto, @MappingTarget Collection collection);

    @Mapping(target = "collection", ignore = true)
    @Mapping(target = "fileData", ignore = true)
    SourceMaterial toEntity(CollectionDto.SourceMaterialDto dto);

    @Mapping(target = "id", source = "sourceMaterial.id")
    @Mapping(target = "filename", source = "sourceMaterial.filename")
    @Mapping(target = "fileType", source = "sourceMaterial.fileType")
    @Mapping(target = "fileSizeBytes", source = "sourceMaterial.fileSizeBytes")
    @Mapping(target = "uploadedAt", source = "sourceMaterial.uploadedAt")
    CollectionDto.SourceMaterialDto toDto(GenerationSourceMaterial gsm);


    GenerationDto toDto(Generation generation);

    @Mapping(target = "sourceMaterials", ignore = true)
    @Mapping(target = "collection", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    Generation toEntity(GenerationDto dto);
}
