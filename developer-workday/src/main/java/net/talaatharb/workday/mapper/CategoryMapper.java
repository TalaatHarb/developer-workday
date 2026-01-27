package net.talaatharb.workday.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import net.talaatharb.workday.dtos.CategoryDTO;
import net.talaatharb.workday.model.Category;

@Mapper
public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);
    
    CategoryDTO toDTO(Category category);
    
    Category toEntity(CategoryDTO dto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDTO(CategoryDTO dto, @MappingTarget Category category);
}
