package net.talaatharb.workday.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category entity for organizing tasks into groups.
 * Implements Serializable for MapDB storage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String name;
    private String description;
    
    private String color; // Hex code (e.g., "#FF5733")
    private String icon; // Icon identifier
    
    private UUID parentCategoryId; // For nested categories
    
    private Integer sortOrder;
    
    @Builder.Default
    private Boolean isDefault = false;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
