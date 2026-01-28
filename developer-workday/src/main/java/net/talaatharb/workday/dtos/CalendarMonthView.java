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
 * DTO representing tasks grouped by date for calendar month view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarMonthView {
    private int year;
    private int month;
    
    @Builder.Default
    private List<DayWithTasks> days = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayWithTasks {
        private LocalDate date;
        
        @Builder.Default
        private List<Task> tasks = new ArrayList<>();
    }
}
