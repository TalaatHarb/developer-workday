package net.talaatharb.workday.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import net.talaatharb.workday.dtos.TaskDTO;
import net.talaatharb.workday.model.Task;

@Mapper(uses = SubtaskMapper.class)
public interface TaskMapper {
    TaskMapper INSTANCE = Mappers.getMapper(TaskMapper.class);
    
    TaskDTO toDTO(Task task);
    
    Task toEntity(TaskDTO dto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDTO(TaskDTO dto, @MappingTarget Task task);
}
