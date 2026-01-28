package net.talaatharb.workday.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CategoryTest {
    
    @Test
    void testCategoryHasAllRequiredFields() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        Category category = Category.builder()
            .id(id)
            .name("Work")
            .description("Work related tasks")
            .color("#FF5733")
            .icon("briefcase")
            .parentCategoryId(parentId)
            .sortOrder(1)
            .isDefault(true)
            .createdAt(now)
            .updatedAt(now)
            .build();
        
        assertEquals(id, category.getId());
        assertEquals("Work", category.getName());
        assertEquals("Work related tasks", category.getDescription());
        assertEquals("#FF5733", category.getColor());
        assertEquals("briefcase", category.getIcon());
        assertEquals(parentId, category.getParentCategoryId());
        assertEquals(1, category.getSortOrder());
        assertTrue(category.getIsDefault());
        assertEquals(now, category.getCreatedAt());
        assertEquals(now, category.getUpdatedAt());
    }
    
    @Test
    void testCategoryIsSerializable() throws Exception {
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name("Personal")
            .description("Personal tasks")
            .color("#00FF00")
            .icon("home")
            .sortOrder(2)
            .isDefault(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(category);
        oos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Category deserializedCategory = (Category) ois.readObject();
        ois.close();
        
        assertEquals(category.getId(), deserializedCategory.getId());
        assertEquals(category.getName(), deserializedCategory.getName());
        assertEquals(category.getDescription(), deserializedCategory.getDescription());
        assertEquals(category.getColor(), deserializedCategory.getColor());
        assertEquals(category.getIcon(), deserializedCategory.getIcon());
    }
    
    @Test
    void testCategoryDefaultValues() {
        Category category = new Category();
        assertNull(category.getId());
        assertNull(category.getName());
        assertFalse(category.getIsDefault());
    }
    
    @Test
    void testNestedCategories() {
        UUID parentId = UUID.randomUUID();
        
        Category parent = Category.builder()
            .id(parentId)
            .name("Projects")
            .build();
        
        Category child = Category.builder()
            .id(UUID.randomUUID())
            .name("Project A")
            .parentCategoryId(parentId)
            .build();
        
        assertNull(parent.getParentCategoryId());
        assertEquals(parentId, child.getParentCategoryId());
    }
    
    @Test
    void testCategoryBuilder() {
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name("Test Category")
            .build();
        
        assertNotNull(category);
        assertNotNull(category.getId());
        assertEquals("Test Category", category.getName());
        assertFalse(category.getIsDefault());
    }
}
