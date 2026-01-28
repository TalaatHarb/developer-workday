package net.talaatharb.workday.dtos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a node in the category tree hierarchy.
 * Contains category information and its children.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNode {
    private UUID id;
    private String name;
    private String description;
    private String color;
    private String icon;
    private UUID parentCategoryId;
    private Integer sortOrder;
    
    @Builder.Default
    private List<CategoryTreeNode> children = new ArrayList<>();
}
