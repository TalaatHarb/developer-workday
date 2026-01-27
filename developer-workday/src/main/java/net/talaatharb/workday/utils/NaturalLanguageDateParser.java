package net.talaatharb.workday.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for parsing natural language date and time expressions.
 * Supports expressions like "tomorrow", "next Friday", "3pm", etc.
 */
@Slf4j
public class NaturalLanguageDateParser {
    
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "\\b(\\d{1,2})(:(\\d{2}))?(\\s*([ap]m))?\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Parse a natural language date expression into a LocalDate.
     * 
     * Supported expressions:
     * - "today" → current date
     * - "tomorrow" → next day
     * - "monday", "tuesday", etc. → next occurrence of that weekday
     * - "next monday", "next tuesday", etc. → next occurrence (if today is Monday, next Monday is 7 days away)
     * - "next week" → 7 days from now
     * - "next month" → same day next month
     * 
     * @param input the natural language date expression
     * @return the parsed LocalDate, or current date if parsing fails
     */
    public static LocalDate parseRelativeDate(String input) {
        if (input == null || input.trim().isEmpty()) {
            return LocalDate.now();
        }
        
        String normalized = input.toLowerCase().trim();
        
        // Handle "today"
        if (normalized.equals("today")) {
            log.debug("Parsed 'today' as {}", LocalDate.now());
            return LocalDate.now();
        }
        
        // Handle "tomorrow"
        if (normalized.equals("tomorrow")) {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            log.debug("Parsed 'tomorrow' as {}", tomorrow);
            return tomorrow;
        }
        
        // Handle "next week"
        if (normalized.equals("next week")) {
            LocalDate nextWeek = LocalDate.now().plusWeeks(1);
            log.debug("Parsed 'next week' as {}", nextWeek);
            return nextWeek;
        }
        
        // Handle "next month"
        if (normalized.equals("next month")) {
            LocalDate nextMonth = LocalDate.now().plusMonths(1);
            log.debug("Parsed 'next month' as {}", nextMonth);
            return nextMonth;
        }
        
        // Handle day names with optional "next" prefix
        boolean hasNextPrefix = normalized.startsWith("next ");
        String dayName = hasNextPrefix ? normalized.substring(5).trim() : normalized;
        
        DayOfWeek targetDay = parseDayOfWeek(dayName);
        if (targetDay != null) {
            return calculateNextOccurrence(targetDay, hasNextPrefix);
        }
        
        log.warn("Could not parse date expression '{}', defaulting to today", input);
        return LocalDate.now();
    }
    
    /**
     * Parse a natural language time expression into a LocalTime.
     * 
     * Supported formats:
     * - "3pm" → 15:00
     * - "3:30pm" → 15:30
     * - "15:00" → 15:00
     * - "8am" → 08:00
     * 
     * @param input the natural language time expression
     * @return the parsed LocalTime, or null if parsing fails
     */
    public static LocalTime parseNaturalTime(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        String normalized = input.toLowerCase().trim();
        
        // Remove "at" prefix if present
        if (normalized.startsWith("at ")) {
            normalized = normalized.substring(3).trim();
        }
        
        Matcher matcher = TIME_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            log.warn("Could not parse time expression '{}'", input);
            return null;
        }
        
        try {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = 0;
            
            // Parse minutes if present
            if (matcher.group(3) != null) {
                minute = Integer.parseInt(matcher.group(3));
            }
            
            // Handle AM/PM
            String amPm = matcher.group(5);
            if (amPm != null) {
                amPm = amPm.toLowerCase();
                if (amPm.equals("pm") && hour < 12) {
                    hour += 12;
                } else if (amPm.equals("am") && hour == 12) {
                    hour = 0;
                }
            }
            
            // Validate hour and minute ranges
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                log.warn("Invalid time values: hour={}, minute={}", hour, minute);
                return null;
            }
            
            LocalTime result = LocalTime.of(hour, minute);
            log.debug("Parsed time '{}' as {}", input, result);
            return result;
            
        } catch (NumberFormatException e) {
            log.warn("Failed to parse time expression '{}'", input, e);
            return null;
        }
    }
    
    /**
     * Parse a day of week name into a DayOfWeek enum.
     */
    private static DayOfWeek parseDayOfWeek(String dayName) {
        return switch (dayName) {
            case "monday", "mon" -> DayOfWeek.MONDAY;
            case "tuesday", "tue", "tues" -> DayOfWeek.TUESDAY;
            case "wednesday", "wed" -> DayOfWeek.WEDNESDAY;
            case "thursday", "thu", "thur", "thurs" -> DayOfWeek.THURSDAY;
            case "friday", "fri" -> DayOfWeek.FRIDAY;
            case "saturday", "sat" -> DayOfWeek.SATURDAY;
            case "sunday", "sun" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
    
    /**
     * Calculate the next occurrence of a given day of the week.
     * 
     * @param targetDay the target day of the week
     * @param hasNextPrefix true if "next" was explicitly specified
     * @return the date of the next occurrence
     */
    private static LocalDate calculateNextOccurrence(DayOfWeek targetDay, boolean hasNextPrefix) {
        LocalDate today = LocalDate.now();
        DayOfWeek currentDay = today.getDayOfWeek();
        
        if (hasNextPrefix) {
            // "next Monday" always means at least 7 days from now if today is Monday
            // Otherwise, it means the next occurrence
            if (currentDay == targetDay) {
                // If today is the target day, go to next week
                LocalDate result = today.plusWeeks(1);
                log.debug("Parsed 'next {}' (today is {}) as {}", targetDay, currentDay, result);
                return result;
            } else {
                // Go to next occurrence
                LocalDate result = today.with(TemporalAdjusters.next(targetDay));
                log.debug("Parsed 'next {}' as {}", targetDay, result);
                return result;
            }
        } else {
            // Without "next" prefix, go to the next occurrence (could be today if it's the same day)
            if (currentDay == targetDay) {
                // If it's already that day, use today
                log.debug("Parsed '{}' (today) as {}", targetDay, today);
                return today;
            } else {
                // Go to next occurrence
                LocalDate result = today.with(TemporalAdjusters.next(targetDay));
                log.debug("Parsed '{}' as {}", targetDay, result);
                return result;
            }
        }
    }
}
