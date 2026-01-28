package net.talaatharb.workday.dtos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.talaatharb.workday.model.Task;

/**
 * DTO representing tasks grouped by date and time slots for calendar week view.
 * Separates all-day tasks from timed tasks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarWeekView {
    private LocalDate startDate;
    private LocalDate endDate;
    
    @Builder.Default
    private List<DayWithTaskSlots> days = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayWithTaskSlots {
        private LocalDate date;
        
        @Builder.Default
        private List<Task> allDayTasks = new ArrayList<>();
        
        @Builder.Default
        private List<Task> timedTasks = new ArrayList<>();
    }
}
