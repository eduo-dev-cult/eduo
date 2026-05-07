package se.ltu.eduo.mapper;

import org.mapstruct.*;
import se.ltu.eduo.dto.ProjectDto;
import se.ltu.eduo.model.project.Project;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {
    Project toEntity(ProjectDto projectDto);

    @AfterMapping
    default void linkSourceMaterials(@MappingTarget Project project) {
        project.getSourceMaterials().forEach(sourceMaterial -> sourceMaterial.setProject(project));
    }

    @AfterMapping
    default void linkGenerations(@MappingTarget Project project) {
        project.getGenerations().forEach(generation -> generation.setProject(project));
    }

    ProjectDto toDto(Project project);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Project partialUpdate(ProjectDto projectDto, @MappingTarget Project project);
}