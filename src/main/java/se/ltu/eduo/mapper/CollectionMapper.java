package se.ltu.eduo.mapper;

import org.mapstruct.*;
import se.ltu.eduo.dto.CollectionDto;
import se.ltu.eduo.model.collection.Collection;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CollectionMapper {
    Collection toEntity(CollectionDto collectionDto);

    @AfterMapping
    default void linkSourceMaterials(@MappingTarget Collection collection) {
        collection.getSourceMaterials().forEach(sourceMaterial -> sourceMaterial.setCollection(collection));
    }

    @AfterMapping
    default void linkGenerations(@MappingTarget Collection collection) {
        collection.getGenerations().forEach(generation -> generation.setCollection(collection));
    }

    CollectionDto toDto(Collection collection);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Collection partialUpdate(CollectionDto collectionDto, @MappingTarget Collection collection);
}