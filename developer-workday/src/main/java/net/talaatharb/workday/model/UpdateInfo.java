package net.talaatharb.workday.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing information about an application update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String currentVersion;
    private String latestVersion;
    private boolean updateAvailable;
    private String downloadUrl;
    private String releaseNotes;
    private LocalDateTime releaseDate;
    private boolean critical; // If true, update is strongly recommended
}
