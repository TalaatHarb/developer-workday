package net.talaatharb.workday.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import net.talaatharb.workday.model.Category;

class CategoryRepositoryTest {
    
    @TempDir
    Path tempDir;
    
    private DB database;
    private CategoryRepository repository;
    
    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test.db");
        database = DBMaker.fileDB(dbPath.toFile()).transactionEnable().make();
        repository = new CategoryRepository(database);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
    }
    
    @Test
    void testSaveAndFindById() {
        Category category = Category.builder()
            .name("Work")
            .description("Work category")
            .color("#FF0000")
            .sortOrder(1)
            .build();
        
        Category saved = repository.save(category);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        
        Category found = repository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Work", found.getName());
    }
    
    @Test
    void testFindAllOrdered() {
        repository.save(Category.builder().name("C").sortOrder(3).build());
        repository.save(Category.builder().name("A").sortOrder(1).build());
        repository.save(Category.builder().name("B").sortOrder(2).build());
        
        List<Category> ordered = repository.findAllOrdered();
        assertEquals(3, ordered.size());
        assertEquals("A", ordered.get(0).getName());
        assertEquals("B", ordered.get(1).getName());
        assertEquals("C", ordered.get(2).getName());
    }
    
    @Test
    void testFindByParentId() {
        UUID parentId = repository.save(Category.builder().name("Parent").build()).getId();
        
        repository.save(Category.builder().name("Child1").parentCategoryId(parentId).build());
        repository.save(Category.builder().name("Child2").parentCategoryId(parentId).build());
        repository.save(Category.builder().name("Other").build());
        
        List<Category> children = repository.findByParentId(parentId);
        assertEquals(2, children.size());
    }
    
    @Test
    void testFindRootCategories() {
        UUID parentId = repository.save(Category.builder().name("Parent").sortOrder(1).build()).getId();
        repository.save(Category.builder().name("Root").sortOrder(2).build());
        repository.save(Category.builder().name("Child").parentCategoryId(parentId).build());
        
        List<Category> roots = repository.findRootCategories();
        assertEquals(2, roots.size());
        assertTrue(roots.stream().allMatch(c -> c.getParentCategoryId() == null));
    }
    
    @Test
    void testFindDefaultCategories() {
        repository.save(Category.builder().name("Default1").isDefault(true).build());
        repository.save(Category.builder().name("Default2").isDefault(true).build());
        repository.save(Category.builder().name("Custom").isDefault(false).build());
        
        List<Category> defaults = repository.findDefaultCategories();
        assertEquals(2, defaults.size());
    }
    
    @Test
    void testDeleteById() {
        UUID id = repository.save(Category.builder().name("ToDelete").build()).getId();
        assertTrue(repository.existsById(id));
        assertTrue(repository.deleteById(id));
        assertFalse(repository.existsById(id));
    }
    
    @Test
    void testCount() {
        assertEquals(0, repository.count());
        repository.save(Category.builder().name("Cat1").build());
        repository.save(Category.builder().name("Cat2").build());
        assertEquals(2, repository.count());
    }
}
