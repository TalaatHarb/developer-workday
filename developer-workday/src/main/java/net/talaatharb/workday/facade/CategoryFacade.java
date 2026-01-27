package net.talaatharb.workday.facade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.CategoryTreeNode;
import net.talaatharb.workday.dtos.CategoryWithTaskCount;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.service.CategoryService;
import net.talaatharb.workday.service.TaskService;

/**
 * Facade for category-related operations, coordinating between multiple services.
 * Provides a simplified interface for UI controllers.
 */
@Slf4j
@RequiredArgsConstructor
public class CategoryFacade {
    
    private final CategoryService categoryService;
    private final TaskService taskService;
    
    /**
     * Get category tree with hierarchical structure.
     * Returns root categories with their nested children.
     */
    public List<CategoryTreeNode> getCategoryTree() {
        log.debug("Getting category tree");
        
        // Get all categories
        List<Category> allCategories = categoryService.findAllOrdered();
        
        // Build tree structure
        List<CategoryTreeNode> tree = buildCategoryTree(allCategories);
        
        log.debug("Built category tree with {} root nodes", tree.size());
        return tree;
    }
    
    /**
     * Get all categories with their active task counts.
     * Active tasks are those not completed or cancelled.
     */
    public List<CategoryWithTaskCount> getCategoriesWithTaskCount() {
        log.debug("Getting categories with task counts");
        
        // Get all categories
        List<Category> categories = categoryService.findAllOrdered();
        
        // Build result with task counts
        List<CategoryWithTaskCount> result = categories.stream()
            .map(this::buildCategoryWithTaskCount)
            .collect(Collectors.toList());
        
        log.debug("Retrieved {} categories with task counts", result.size());
        return result;
    }
    
    /**
     * Build hierarchical tree structure from flat category list
     */
    private List<CategoryTreeNode> buildCategoryTree(List<Category> categories) {
        // Create map of category ID to tree node
        Map<java.util.UUID, CategoryTreeNode> nodeMap = new HashMap<>();
        
        // Create nodes for all categories
        for (Category category : categories) {
            CategoryTreeNode node = CategoryTreeNode.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .color(category.getColor())
                .icon(category.getIcon())
                .parentCategoryId(category.getParentCategoryId())
                .sortOrder(category.getSortOrder())
                .children(new ArrayList<>())
                .build();
            nodeMap.put(category.getId(), node);
        }
        
        // Build tree by linking children to parents
        List<CategoryTreeNode> rootNodes = new ArrayList<>();
        for (Category category : categories) {
            CategoryTreeNode node = nodeMap.get(category.getId());
            
            if (category.getParentCategoryId() == null) {
                // Root category
                rootNodes.add(node);
            } else {
                // Child category - add to parent
                CategoryTreeNode parent = nodeMap.get(category.getParentCategoryId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // Parent not found, treat as root
                    rootNodes.add(node);
                }
            }
        }
        
        return rootNodes;
    }
    
    /**
     * Build CategoryWithTaskCount from a Category
     */
    private CategoryWithTaskCount buildCategoryWithTaskCount(Category category) {
        // Count active tasks (not completed or cancelled)
        long taskCount = taskService.findByCategoryId(category.getId()).stream()
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED 
                         && task.getStatus() != TaskStatus.CANCELLED)
            .count();
        
        return CategoryWithTaskCount.builder()
            .id(category.getId())
            .name(category.getName())
            .description(category.getDescription())
            .color(category.getColor())
            .icon(category.getIcon())
            .parentCategoryId(category.getParentCategoryId())
            .sortOrder(category.getSortOrder())
            .isDefault(category.getIsDefault())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .taskCount(taskCount)
            .build();
    }
}
