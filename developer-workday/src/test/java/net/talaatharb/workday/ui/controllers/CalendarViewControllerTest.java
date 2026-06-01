package net.talaatharb.workday.ui.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.facade.CalendarFacade;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Tests for CalendarViewController.
 * The calendar view is now backed by CalendarFX which provides built-in
 * navigation and month/week/day views out of the box.
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
    @DisplayName("FXML loads successfully and CalendarFX view is added to container")
    void testFxmlLoadsWithCalendarFxView() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                assertNotNull(fxmlResource, "FXML file should exist");

                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                CalendarViewController controller = loader.getController();
                assertNotNull(controller, "Controller should be initialized");

                VBox calendarContainer = (VBox) root.lookup("#calendarContainer");
                assertNotNull(calendarContainer, "Calendar container should exist");

                assertFalse(calendarContainer.getChildren().isEmpty(),
                        "CalendarFX view should be added to container on initialize");

            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });

        Thread.sleep(500);
    }

    @Test
    @DisplayName("loadCalendarTasks calls the calendar facade for a date range")
    void testLoadCalendarTasksCallsFacade() throws Exception {
        Platform.runLater(() -> {
            try {
                when(mockCalendarFacade.getTasksForPeriod(any(LocalDate.class), any(LocalDate.class)))
                        .thenReturn(createSampleTasks());

                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                loader.load();
                CalendarViewController controller = loader.getController();

                controller.setCalendarFacade(mockCalendarFacade);
                controller.setTaskFacade(mockTaskFacade);

                controller.loadCalendarTasks();

                Thread.sleep(200);

                verify(mockCalendarFacade, atLeastOnce())
                        .getTasksForPeriod(any(LocalDate.class), any(LocalDate.class));

            } catch (Exception e) {
                fail("Test failed: " + e.getMessage());
            }
        });

        Thread.sleep(700);
    }

    @Test
    @DisplayName("loadCalendarTasks with no facade set does nothing")
    void testLoadCalendarTasksWithNoFacade() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                loader.load();
                CalendarViewController controller = loader.getController();

                assertDoesNotThrow(controller::loadCalendarTasks);

            } catch (Exception e) {
                fail("Test failed: " + e.getMessage());
            }
        });

        Thread.sleep(500);
    }

    @Test
    @DisplayName("handleDayClick fetches tasks for clicked day and shows dialog")
    void testHandleDayClickShowsDialog() throws Exception {
        Platform.runLater(() -> {
            try {
                LocalDate clickedDay = LocalDate.now();
                List<Task> tasks = createSampleTasks();
                when(mockCalendarFacade.getTasksForDay(clickedDay)).thenReturn(tasks);
                when(mockCalendarFacade.getTasksForPeriod(any(LocalDate.class), any(LocalDate.class)))
                        .thenReturn(new ArrayList<>());

                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CalendarView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                loader.load();
                CalendarViewController real = loader.getController();
                CalendarViewController controller = spy(real);
                controller.setCalendarFacade(mockCalendarFacade);
                controller.setTaskFacade(mockTaskFacade);

                javafx.scene.control.Alert stubAlert = mock(javafx.scene.control.Alert.class);
                when(stubAlert.showAndWait()).thenReturn(java.util.Optional.empty());
                doReturn(stubAlert).when(controller).createDayTasksDialog(any(LocalDate.class), anyList());

                controller.handleDayClick(clickedDay);

                verify(mockCalendarFacade).getTasksForDay(clickedDay);
                verify(controller).createDayTasksDialog(eq(clickedDay), anyList());
                verify(stubAlert).showAndWait();
            } catch (Exception e) {
                fail("Failed to test handleDayClick: " + e.getMessage());
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
