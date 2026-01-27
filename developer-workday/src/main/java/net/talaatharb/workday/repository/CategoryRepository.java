package net.talaatharb.workday.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mapdb.DB;
import org.mapdb.Serializer;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Category;

@Slf4j
public class CategoryRepository {
    private static final String CATEGORIES_MAP = "categories";
    private final DB database;
    private final Map<UUID, Category> categoriesMap;
    
    public CategoryRepository(DB database) {
        this.database = database;
        this.categoriesMap = database.hashMap(CATEGORIES_MAP, Serializer.UUID, Serializer.JAVA).createOrOpen();
        log.info("CategoryRepository initialized with {} categories", categoriesMap.size());
    }
    
    public Category save(Category category) {
        if (category.getId() == null) {
            category.setId(UUID.randomUUID());
        }
        
        if (category.getCreatedAt() == null) {
            category.setCreatedAt(LocalDateTime.now());
        }
        category.setUpdatedAt(LocalDateTime.now());
        
        categoriesMap.put(category.getId(), category);
        database.commit();
        log.debug("Saved category: {}", category.getId());
        return category;
    }
    
    public Optional<Category> findById(UUID id) {
        return Optional.ofNullable(categoriesMap.get(id));
    }
    
    public List<Category> findAll() {
        return new ArrayList<>(categoriesMap.values());
    }
    
    public List<Category> findAllOrdered() {
        return categoriesMap.values().stream()
            .sorted(Comparator.comparing(Category::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }
    
    public List<Category> findByParentId(UUID parentId) {
        return categoriesMap.values().stream()
            .filter(cat -> parentId != null && parentId.equals(cat.getParentCategoryId()))
            .collect(Collectors.toList());
    }
    
    public List<Category> findRootCategories() {
        return categoriesMap.values().stream()
            .filter(cat -> cat.getParentCategoryId() == null)
            .sorted(Comparator.comparing(Category::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }
    
    public List<Category> findDefaultCategories() {
        return categoriesMap.values().stream()
            .filter(cat -> Boolean.TRUE.equals(cat.getIsDefault()))
            .collect(Collectors.toList());
    }
    
    public boolean deleteById(UUID id) {
        Category removed = categoriesMap.remove(id);
        if (removed != null) {
            database.commit();
            log.debug("Deleted category: {}", id);
            return true;
        }
        return false;
    }
    
    public void deleteAll() {
        categoriesMap.clear();
        database.commit();
        log.debug("Deleted all categories");
    }
    
    public long count() {
        return categoriesMap.size();
    }
    
    public boolean existsById(UUID id) {
        return categoriesMap.containsKey(id);
    }
}
