package net.talaatharb.workday.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for focus mode state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusModeDTO {
    private boolean enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer timerDurationMinutes;
    private Integer remainingMinutes;
    private Integer breakReminderIntervalMinutes;
}
