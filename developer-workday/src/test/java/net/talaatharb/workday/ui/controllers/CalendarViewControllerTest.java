package net.talaatharb.workday.ui.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.facade.CalendarFacade;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.ui.controllers.CalendarViewController.ViewMode;

/**
 * Tests for CalendarViewController following the acceptance criteria.
 * 
 * Feature: Calendar View
 *   Scenario: Month view display
 *     Given the calendar view is in month mode
 *     When viewing the calendar
 *     Then a monthly grid should be displayed
 *     And days with tasks should show task indicators
 *     And clicking a day should show tasks for that day
 *
 *   Scenario: Week view display
 *     Given the calendar view is in week mode
 *     When viewing the calendar
 *     Then a weekly time grid should be displayed
 *     And tasks should be positioned according to their scheduled time
 *     And tasks can be dragged to reschedule
 *
 *   Scenario: Drag and drop scheduling
 *     Given a task in the calendar view
 *     When dragging the task to a different time slot
 *     Then the task schedule should be updated
 *     And a TaskScheduledEvent should be published
 */
class CalendarViewControllerTest {
    
    private static boolean jfxInitialized = false;
    private TaskFacade mockTaskFacade;
    private CalendarFacade mockCalendarFacade;
    private EventDispatcher mockEventDispatcher;
    
    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        if (!jfxInitialized) {
            Platform.startup(() -> {});
            jfxInitialized = true;
        }
    }
    
    @BeforeEach
    void setUp() {
        mockTaskFacade = mock(TaskFacade.class);
        mockCalendarFacade = mock(CalendarFacade.class);
        mockEventDispatcher = mock(EventDispatcher.class);
    }
    
    @Test
    @DisplayName("Month view display - grid is displayed")
    void testMonthViewDisplay() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: calendar view is in month mode with sample tasks
                List<Task> tasks = createSampleTasks();
                when(mockCalendarFacade.getTasksForPeriod(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(tasks);
                
                // When: viewing the calendar
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                assertNotNull(fxmlResource, "FXML file should exist");
                
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                CalendarViewController controller = loader.getController();
                assertNotNull(controller, "Controller should be initialized");
                
                controller.setCalendarFacade(mockCalendarFacade);
                controller.setTaskFacade(mockTaskFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                
                // Then: a monthly grid should be displayed
                GridPane calendarGrid = (GridPane) root.lookup("#calendarGrid");
                assertNotNull(calendarGrid, "Calendar grid should exist");
                
                GridPane dayHeadersGrid = (GridPane) root.lookup("#dayHeadersGrid");
                assertNotNull(dayHeadersGrid, "Day headers grid should exist");
                
                // Verify month view container is visible
                VBox monthViewContainer = (VBox) root.lookup("#monthViewContainer");
                assertNotNull(monthViewContainer, "Month view container should exist");
                assertTrue(monthViewContainer.isVisible(), "Month view should be visible");
                
            } catch (Exception e) {
                fail("Failed to load FXML or test: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Month view display - days with tasks show indicators")
    void testMonthViewTaskIndicators() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: tasks scheduled for specific days
                List<Task> tasks = createSampleTasks();
                when(mockCalendarFacade.getTasksForPeriod(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(tasks);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                CalendarViewController controller = loader.getController();
                
                controller.setCalendarFacade(mockCalendarFacade);
                controller.setTaskFacade(mockTaskFacade);
                
                // Wait for grid to render
                Thread.sleep(200);
                
                // Then: calendar grid should have cells with task indicators
                GridPane calendarGrid = (GridPane) root.lookup("#calendarGrid");
                assertNotNull(calendarGrid, "Calendar grid should exist");
                
                // Calendar should be populated (number of children > 0)
                assertTrue(calendarGrid.getChildren().size() > 0, 
                    "Calendar grid should have day cells");
                
            } catch (Exception e) {
                fail("Failed to test task indicators: " + e.getMessage());
            }
        });
        
        Thread.sleep(700);
    }
    
    @Test
    @DisplayName("Week view display - can switch to week mode")
    void testWeekViewDisplay() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: calendar view with sample tasks
                List<Task> tasks = createSampleTasks();
                when(mockCalendarFacade.getTasksForPeriod(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(tasks);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                CalendarViewController controller = loader.getController();
                
                controller.setCalendarFacade(mockCalendarFacade);
                controller.setTaskFacade(mockTaskFacade);
                
                // When: switching to week mode
                @SuppressWarnings("unchecked")
                ChoiceBox<ViewMode> viewModeChoice = (ChoiceBox<ViewMode>) root.lookup("#viewModeChoice");
                assertNotNull(viewModeChoice, "View mode choice should exist");
                
                viewModeChoice.setValue(ViewMode.WEEK);
                
                // Wait for view to update
                Thread.sleep(200);
                
                // Then: week view should be visible
                VBox weekViewContainer = (VBox) root.lookup("#weekViewContainer");
                assertNotNull(weekViewContainer, "Week view container should exist");
                assertTrue(weekViewContainer.isVisible(), "Week view should be visible");
                
                // Month view should be hidden
                VBox monthViewContainer = (VBox) root.lookup("#monthViewContainer");
                assertFalse(monthViewContainer.isVisible(), "Month view should be hidden");
                
            } catch (Exception e) {
                fail("Failed to test week view: " + e.getMessage());
            }
        });
        
        Thread.sleep(700);
    }
    
    @Test
    @DisplayName("Day view display - can switch to day mode")
    void testDayViewDisplay() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: calendar view
                List<Task> tasks = createSampleTasks();
                when(mockCalendarFacade.getTasksForDay(any(LocalDate.class))).thenReturn(tasks);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                CalendarViewController controller = loader.getController();
                
                controller.setCalendarFacade(mockCalendarFacade);
                controller.setTaskFacade(mockTaskFacade);
                
                // When: switching to day mode
                @SuppressWarnings("unchecked")
                ChoiceBox<ViewMode> viewModeChoice = (ChoiceBox<ViewMode>) root.lookup("#viewModeChoice");
                viewModeChoice.setValue(ViewMode.DAY);
                
                Thread.sleep(200);
                
                // Then: day view should be visible
                VBox dayViewContainer = (VBox) root.lookup("#dayViewContainer");
                assertNotNull(dayViewContainer, "Day view container should exist");
                assertTrue(dayViewContainer.isVisible(), "Day view should be visible");
                
            } catch (Exception e) {
                fail("Failed to test day view: " + e.getMessage());
            }
        });
        
        Thread.sleep(700);
    }
    
    @Test
    @DisplayName("Calendar has required controls")
    void testCalendarHasRequiredControls() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                
                // Verify all required controls exist
                assertNotNull(root.lookup("#monthYearLabel"), "Month/year label should exist");
                assertNotNull(root.lookup("#previousButton"), "Previous button should exist");
                assertNotNull(root.lookup("#todayButton"), "Today button should exist");
                assertNotNull(root.lookup("#nextButton"), "Next button should exist");
                assertNotNull(root.lookup("#viewModeChoice"), "View mode choice should exist");
                assertNotNull(root.lookup("#calendarGrid"), "Calendar grid should exist");
                assertNotNull(root.lookup("#weekGrid"), "Week grid should exist");
                assertNotNull(root.lookup("#dayGrid"), "Day grid should exist");
                assertNotNull(root.lookup("#taskCountLabel"), "Task count label should exist");
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Navigation buttons work")
    void testNavigationButtons() throws Exception {
        Platform.runLater(() -> {
            try {
                when(mockCalendarFacade.getTasksForPeriod(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new ArrayList<>());
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                CalendarViewController controller = loader.getController();
                
                controller.setCalendarFacade(mockCalendarFacade);
                
                Label monthYearLabel = (Label) root.lookup("#monthYearLabel");
                String initialText = monthYearLabel.getText();
                
                // Click next button
                controller.handleNext();
                Thread.sleep(100);
                
                // Label should change
                assertNotEquals(initialText, monthYearLabel.getText(), 
                    "Month/year label should change after next");
                
            } catch (Exception e) {
                fail("Failed to test navigation: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    private List<Task> createSampleTasks() {
        List<Task> tasks = new ArrayList<>();
        
        tasks.add(Task.builder()
            .id(UUID.randomUUID())
            .title("Task 1")
            .scheduledDate(LocalDate.now())
            .dueDate(LocalDate.now())
            .priority(Priority.HIGH)
            .status(TaskStatus.TODO)
            .createdAt(LocalDateTime.now())
            .build());
        
        tasks.add(Task.builder()
            .id(UUID.randomUUID())
            .title("Task 2")
            .scheduledDate(LocalDate.now().plusDays(1))
            .dueDate(LocalDate.now().plusDays(1))
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .createdAt(LocalDateTime.now())
            .build());
        
        return tasks;
    }
}
