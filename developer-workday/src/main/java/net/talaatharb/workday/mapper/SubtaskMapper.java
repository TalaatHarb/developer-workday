package net.talaatharb.workday.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import net.talaatharb.workday.dtos.SubtaskDTO;
import net.talaatharb.workday.model.Subtask;

@Mapper
public interface SubtaskMapper {
    SubtaskMapper INSTANCE = Mappers.getMapper(SubtaskMapper.class);
    
    SubtaskDTO toDTO(Subtask subtask);
    
    Subtask toEntity(SubtaskDTO dto);
}
