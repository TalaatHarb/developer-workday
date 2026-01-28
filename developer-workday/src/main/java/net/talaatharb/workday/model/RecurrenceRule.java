package net.talaatharb.workday.model;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recurrence rule for repeating tasks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurrenceRule implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private RecurrenceType type;
    private Integer interval; // Every N days/weeks/months/years
    private Set<DayOfWeek> daysOfWeek; // For weekly recurrence
    private Integer dayOfMonth; // For monthly recurrence
    private LocalDate endDate; // When recurrence stops
    private Integer maxOccurrences; // Max number of occurrences
    
    public enum RecurrenceType {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}
