package net.talaatharb.workday.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Focus mode state model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusModeState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Builder.Default
    private boolean enabled = false;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    @Builder.Default
    private Integer timerDurationMinutes = null; // null means no timer
    
    @Builder.Default
    private Integer breakReminderIntervalMinutes = 25; // Pomodoro-style default
    
    private LocalDateTime lastBreakReminderTime;
}
