package net.talaatharb.workday.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.talaatharb.workday.dtos.CategoryDTO;
import net.talaatharb.workday.model.Category;

class CategoryMapperTest {
    
    private final CategoryMapper mapper = CategoryMapper.INSTANCE;
    
    @Test
    void testToDTO() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        Category category = Category.builder()
            .id(id)
            .name("Work")
            .description("Work category")
            .color("#FF0000")
            .icon("briefcase")
            .parentCategoryId(parentId)
            .sortOrder(1)
            .isDefault(true)
            .createdAt(now)
            .updatedAt(now)
            .build();
        
        CategoryDTO dto = mapper.toDTO(category);
        
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("Work", dto.getName());
        assertEquals("#FF0000", dto.getColor());
        assertEquals("briefcase", dto.getIcon());
        assertEquals(parentId, dto.getParentCategoryId());
        assertEquals(1, dto.getSortOrder());
        assertTrue(dto.getIsDefault());
    }
    
    @Test
    void testToEntity() {
        UUID id = UUID.randomUUID();
        
        CategoryDTO dto = CategoryDTO.builder()
            .id(id)
            .name("Personal")
            .description("Personal category")
            .color("#00FF00")
            .sortOrder(2)
            .isDefault(false)
            .build();
        
        Category category = mapper.toEntity(dto);
        
        assertNotNull(category);
        assertEquals(id, category.getId());
        assertEquals("Personal", category.getName());
        assertEquals("#00FF00", category.getColor());
        assertEquals(2, category.getSortOrder());
        assertFalse(category.getIsDefault());
    }
    
    @Test
    void testNullHandling() {
        assertNull(mapper.toDTO(null));
        assertNull(mapper.toEntity(null));
    }
}
