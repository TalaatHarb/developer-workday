package net.talaatharb.workday.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a category with its associated task count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryWithTaskCount {
    private UUID id;
    private String name;
    private String description;
    private String color;
    private String icon;
    private UUID parentCategoryId;
    private Integer sortOrder;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private long taskCount;
}
