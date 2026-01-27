package net.talaatharb.workday.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for NaturalLanguageDateParser following Task 47 acceptance criteria.
 * 
 * Feature: Natural Language Date Parsing
 *   Scenario: Parse relative dates
 *     Given quick add input 'Buy milk tomorrow'
 *     When parsing the input
 *     Then 'tomorrow' should be parsed as the next day's date
 *     And the task should be created with that due date
 *
 *   Scenario: Parse specific times
 *     Given quick add input 'Meeting at 3pm'
 *     When parsing the input
 *     Then '3pm' should be parsed as 15:00
 *     And the task should have that due time
 *
 *   Scenario: Parse complex date expressions
 *     Given quick add input 'Report due next Friday'
 *     When parsing the input
 *     Then 'next Friday' should be parsed correctly
 *     And edge cases like weekends should be handled properly
 */
class NaturalLanguageDateParserTest {
    
    @Test
    @DisplayName("Parse 'today' returns current date")
    void testParseToday() {
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate("today");
        assertEquals(LocalDate.now(), result);
    }
    
    @Test
    @DisplayName("Parse 'tomorrow' returns next day's date")
    void testParseTomorrow() {
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate("tomorrow");
        assertEquals(LocalDate.now().plusDays(1), result);
    }
    
    @Test
    @DisplayName("Parse '3pm' as 15:00")
    void testParse3pm() {
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime("3pm");
        assertEquals(LocalTime.of(15, 0), result);
    }
    
    @Test
    @DisplayName("Parse '3:30pm' as 15:30")
    void testParse330pm() {
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime("3:30pm");
        assertEquals(LocalTime.of(15, 30), result);
    }
    
    @Test
    @DisplayName("Parse '8am' as 08:00")
    void testParse8am() {
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime("8am");
        assertEquals(LocalTime.of(8, 0), result);
    }
    
    @Test
    @DisplayName("Parse '12pm' as noon (12:00)")
    void testParse12pm() {
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime("12pm");
        assertEquals(LocalTime.of(12, 0), result);
    }
    
    @Test
    @DisplayName("Parse '12am' as midnight (00:00)")
    void testParse12am() {
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime("12am");
        assertEquals(LocalTime.of(0, 0), result);
    }
    
    @Test
    @DisplayName("Parse '15:00' as 15:00 (24-hour format)")
    void testParse24Hour() {
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime("15:00");
        assertEquals(LocalTime.of(15, 0), result);
    }
    
    @Test
    @DisplayName("Parse 'at 5pm' with 'at' prefix")
    void testParseWithAtPrefix() {
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime("at 5pm");
        assertEquals(LocalTime.of(17, 0), result);
    }
    
    @Test
    @DisplayName("Parse 'next Friday' correctly")
    void testParseNextFriday() {
        LocalDate today = LocalDate.now();
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate("next Friday");
        
        // Should be at least 1 day in the future
        assertTrue(result.isAfter(today), "Next Friday should be in the future");
        
        // Should be a Friday
        assertEquals(DayOfWeek.FRIDAY, result.getDayOfWeek());
        
        // If today is Friday, next Friday should be 7 days away
        if (today.getDayOfWeek() == DayOfWeek.FRIDAY) {
            assertEquals(today.plusWeeks(1), result);
        }
    }
    
    @Test
    @DisplayName("Parse 'next Monday' correctly")
    void testParseNextMonday() {
        LocalDate today = LocalDate.now();
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate("next Monday");
        
        // Should be a Monday
        assertEquals(DayOfWeek.MONDAY, result.getDayOfWeek());
        
        // Should be in the future
        assertTrue(result.isAfter(today) || result.equals(today.plusWeeks(1)));
    }
    
    @Test
    @DisplayName("Parse 'Friday' (without 'next') correctly")
    void testParseFriday() {
        LocalDate today = LocalDate.now();
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate("Friday");
        
        // Should be a Friday
        assertEquals(DayOfWeek.FRIDAY, result.getDayOfWeek());
        
        // Should be today or in the future
        assertTrue(result.equals(today) || result.isAfter(today));
        
        // If today is Friday, should return today
        if (today.getDayOfWeek() == DayOfWeek.FRIDAY) {
            assertEquals(today, result);
        }
    }
    
    @Test
    @DisplayName("Parse 'next week' returns date 7 days from now")
    void testParseNextWeek() {
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate("next week");
        assertEquals(LocalDate.now().plusWeeks(1), result);
    }
    
    @Test
    @DisplayName("Parse 'next month' returns same day next month")
    void testParseNextMonth() {
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate("next month");
        assertEquals(LocalDate.now().plusMonths(1), result);
    }
    
    @Test
    @DisplayName("Parse all weekday names")
    void testParseAllWeekdays() {
        String[] weekdays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        DayOfWeek[] expectedDays = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };
        
        for (int i = 0; i < weekdays.length; i++) {
            LocalDate result = NaturalLanguageDateParser.parseRelativeDate(weekdays[i]);
            assertEquals(expectedDays[i], result.getDayOfWeek(), 
                "Weekday " + weekdays[i] + " should be parsed correctly");
        }
    }
    
    @Test
    @DisplayName("Parse case-insensitive inputs")
    void testParseCaseInsensitive() {
        assertEquals(LocalDate.now(), NaturalLanguageDateParser.parseRelativeDate("TODAY"));
        assertEquals(LocalDate.now(), NaturalLanguageDateParser.parseRelativeDate("ToDay"));
        assertEquals(LocalDate.now().plusDays(1), NaturalLanguageDateParser.parseRelativeDate("TOMORROW"));
        assertEquals(LocalTime.of(15, 0), NaturalLanguageDateParser.parseNaturalTime("3PM"));
    }
    
    @Test
    @DisplayName("Handle edge case: weekend days")
    void testParseWeekendDays() {
        // Saturday
        LocalDate saturday = NaturalLanguageDateParser.parseRelativeDate("Saturday");
        assertEquals(DayOfWeek.SATURDAY, saturday.getDayOfWeek());
        
        // Sunday
        LocalDate sunday = NaturalLanguageDateParser.parseRelativeDate("Sunday");
        assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());
        
        // Next Saturday
        LocalDate nextSaturday = NaturalLanguageDateParser.parseRelativeDate("next Saturday");
        assertEquals(DayOfWeek.SATURDAY, nextSaturday.getDayOfWeek());
        
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            assertEquals(today.plusWeeks(1), nextSaturday);
        }
    }
    
    @Test
    @DisplayName("Handle invalid date input - defaults to today")
    void testParseInvalidDate() {
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate("invalid input");
        assertEquals(LocalDate.now(), result);
    }
    
    @Test
    @DisplayName("Handle invalid time input - returns null")
    void testParseInvalidTime() {
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime("invalid time");
        assertNull(result);
    }
    
    @Test
    @DisplayName("Handle empty date input - returns today")
    void testParseEmptyDate() {
        LocalDate result1 = NaturalLanguageDateParser.parseRelativeDate("");
        LocalDate result2 = NaturalLanguageDateParser.parseRelativeDate(null);
        
        assertEquals(LocalDate.now(), result1);
        assertEquals(LocalDate.now(), result2);
    }
    
    @Test
    @DisplayName("Handle empty time input - returns null")
    void testParseEmptyTime() {
        LocalTime result1 = NaturalLanguageDateParser.parseNaturalTime("");
        LocalTime result2 = NaturalLanguageDateParser.parseNaturalTime(null);
        
        assertNull(result1);
        assertNull(result2);
    }
    
    @Test
    @DisplayName("Parse abbreviated weekday names")
    void testParseAbbreviatedWeekdays() {
        LocalDate mon = NaturalLanguageDateParser.parseRelativeDate("Mon");
        assertEquals(DayOfWeek.MONDAY, mon.getDayOfWeek());
        
        LocalDate tue = NaturalLanguageDateParser.parseRelativeDate("Tue");
        assertEquals(DayOfWeek.TUESDAY, tue.getDayOfWeek());
        
        LocalDate fri = NaturalLanguageDateParser.parseRelativeDate("Fri");
        assertEquals(DayOfWeek.FRIDAY, fri.getDayOfWeek());
        
        LocalDate sat = NaturalLanguageDateParser.parseRelativeDate("Sat");
        assertEquals(DayOfWeek.SATURDAY, sat.getDayOfWeek());
    }
    
    @Test
    @DisplayName("Comprehensive test: 'Buy milk tomorrow' parsing")
    void testComprehensiveExample1() {
        // Simulating the parsing of 'Buy milk tomorrow'
        String input = "tomorrow";
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate(input);
        
        // Tomorrow should be parsed as the next day's date
        assertEquals(LocalDate.now().plusDays(1), result);
    }
    
    @Test
    @DisplayName("Comprehensive test: 'Meeting at 3pm' parsing")
    void testComprehensiveExample2() {
        // Simulating the parsing of 'Meeting at 3pm'
        String input = "at 3pm";
        LocalTime result = NaturalLanguageDateParser.parseNaturalTime(input);
        
        // 3pm should be parsed as 15:00
        assertEquals(LocalTime.of(15, 0), result);
    }
    
    @Test
    @DisplayName("Comprehensive test: 'Report due next Friday' parsing")
    void testComprehensiveExample3() {
        // Simulating the parsing of 'Report due next Friday'
        String input = "next Friday";
        LocalDate result = NaturalLanguageDateParser.parseRelativeDate(input);
        
        // Next Friday should be parsed correctly
        assertEquals(DayOfWeek.FRIDAY, result.getDayOfWeek());
        
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.FRIDAY) {
            // If today is Friday, next Friday should be 7 days away
            assertEquals(today.plusWeeks(1), result);
        } else {
            // Should be in the future
            assertTrue(result.isAfter(today));
        }
    }
}
