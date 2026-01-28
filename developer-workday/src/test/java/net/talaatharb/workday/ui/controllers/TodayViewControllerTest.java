package net.talaatharb.workday.ui.controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;
import net.talaatharb.workday.service.ReminderService;
import net.talaatharb.workday.service.TaskService;

/**
 * Tests for TodayViewController following acceptance criteria for task #40.
 * 
 * Feature: Today View
 *   Scenario: Today view sections
 *   Scenario: Morning and afternoon grouping
 *   Scenario: Today view empty state
 */
class TodayViewControllerTest extends ApplicationTest {
    
    private TodayViewController controller;
    private DB database;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private TaskService taskService;
    private ReminderService reminderService;
    private TaskFacade taskFacade;
    private File dbFile;
    private VBox rootView;
    
    @BeforeAll
    static void initToolkit() {
        // Ensure JavaFX toolkit is initialized
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        // Create test database
        dbFile = new File("test-todayview-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        
        taskRepository = new TaskRepository(database);
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        taskService = new TaskService(taskRepository, eventDispatcher);
        reminderService = new ReminderService(eventDispatcher);
        taskFacade = new TaskFacade(taskService, reminderService);
        
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/talaatharb/workday/ui/TodayView.fxml"));
        rootView = loader.load();
        controller = loader.getController();
        controller.setTaskFacade(taskFacade);
        
        Scene scene = new Scene(rootView, 800, 600);
        stage.setScene(scene);
        stage.show();
    }
    
    @AfterEach
    void tearDown() {
        if (reminderService != null) {
            reminderService.shutdown();
        }
        if (database != null && !database.isClosed()) {
            database.close();
        }
        if (dbFile != null && dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    @DisplayName("Scenario: Today view sections - should show overdue and today sections")
    void testTodayViewSections() throws Exception {
        // Given: tasks with various due dates
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // Create overdue task
        Task overdueTask = taskRepository.save(Task.builder()
            .title("Overdue Task")
            .status(TaskStatus.TODO)
            .dueDate(yesterday)
            .priority(Priority.HIGH)
            .build());
        
        // Create today's task
        Task todayTask = taskRepository.save(Task.builder()
            .title("Today's Task")
            .status(TaskStatus.TODO)
            .scheduledDate(today)
            .dueDate(today)
            .priority(Priority.MEDIUM)
            .build());
        
        // When: viewing the Today view
        CountDownLatch latch = new CountDownLatch(1);
        interact(() -> {
            controller.loadTasks();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "UI update should complete");
        
        // Then: 'Overdue' section should show past due tasks
        VBox overdueSection = lookup("#overdueSection").query();
        assertNotNull(overdueSection, "Overdue section should exist");
        assertTrue(overdueSection.isVisible(), "Overdue section should be visible");
        assertTrue(overdueSection.isManaged(), "Overdue section should be managed");
        
        // And: 'Today' section should show tasks due today
        VBox todaySection = lookup("#todaySection").query();
        assertNotNull(todaySection, "Today section should exist");
        assertTrue(todaySection.isVisible(), "Today section should be visible");
        assertTrue(todaySection.isManaged(), "Today section should be managed");
        
        // And: sections should be collapsible
        VBox overdueTasksContainer = lookup("#overdueTasksContainer").query();
        assertTrue(overdueTasksContainer.isVisible(), "Overdue tasks should be visible initially");
        
        // Click to collapse overdue section
        clickOn("#overdueSectionToggle");
        sleep(200);
        assertFalse(overdueTasksContainer.isVisible(), "Overdue tasks should be hidden after collapse");
    }
    
    @Test
    @DisplayName("Scenario: Morning and afternoon grouping - tasks grouped by time blocks")
    void testTimeBlockGrouping() throws Exception {
        // Given: tasks scheduled for different times today
        LocalDate today = LocalDate.now();
        
        // Morning task (9 AM)
        Task morningTask = taskRepository.save(Task.builder()
            .title("Morning Standup")
            .status(TaskStatus.TODO)
            .scheduledDate(today)
            .dueDate(today)
            .dueTime(LocalTime.of(9, 0))
            .priority(Priority.MEDIUM)
            .build());
        
        // Afternoon task (2 PM)
        Task afternoonTask = taskRepository.save(Task.builder()
            .title("Code Review")
            .status(TaskStatus.TODO)
            .scheduledDate(today)
            .dueDate(today)
            .dueTime(LocalTime.of(14, 0))
            .priority(Priority.MEDIUM)
            .build());
        
        // Evening task (6 PM)
        Task eveningTask = taskRepository.save(Task.builder()
            .title("Deploy Release")
            .status(TaskStatus.TODO)
            .scheduledDate(today)
            .dueDate(today)
            .dueTime(LocalTime.of(18, 0))
            .priority(Priority.HIGH)
            .build());
        
        // When: viewing Today view
        CountDownLatch latch = new CountDownLatch(1);
        interact(() -> {
            controller.loadTasks();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "UI update should complete");
        
        // Then: tasks should be grouped into time blocks (Morning, Afternoon, Evening)
        VBox todayTasksContainer = lookup("#todayTasksContainer").query();
        assertNotNull(todayTasksContainer, "Today tasks container should exist");
        
        // Verify time blocks are present
        assertTrue(todayTasksContainer.getChildren().size() >= 3, 
            "Should have at least 3 time blocks (Morning, Afternoon, Evening)");
        
        // Verify time block labels exist
        List<String> timeBlockLabels = List.of("Morning", "Afternoon", "Evening");
        for (String label : timeBlockLabels) {
            boolean found = todayTasksContainer.getChildren().stream()
                .anyMatch(node -> {
                    if (node instanceof VBox vbox) {
                        return vbox.getChildren().stream()
                            .anyMatch(child -> child.toString().contains(label));
                    }
                    return false;
                });
            assertTrue(found, "Time block '" + label + "' should be present");
        }
    }
    
    @Test
    @DisplayName("Scenario: Today view empty state - shows friendly message when no tasks")
    void testEmptyState() throws Exception {
        // Given: no tasks for today
        // (database is empty)
        
        // When: viewing Today view
        CountDownLatch latch = new CountDownLatch(1);
        interact(() -> {
            controller.loadTasks();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "UI update should complete");
        
        // Then: a friendly empty state message should be displayed
        VBox emptyStateContainer = lookup("#emptyStateContainer").query();
        assertNotNull(emptyStateContainer, "Empty state container should exist");
        assertTrue(emptyStateContainer.isVisible(), "Empty state should be visible when no tasks");
        assertTrue(emptyStateContainer.isManaged(), "Empty state should be managed when no tasks");
        
        // And: a quick add button should be prominently displayed
        assertNotNull(lookup("#emptyStateQuickAddButton").query(), 
            "Quick add button should be present in empty state");
        
        // Verify sections are hidden when empty
        VBox overdueSection = lookup("#overdueSection").query();
        assertFalse(overdueSection.isVisible(), "Overdue section should be hidden when no tasks");
        
        VBox todaySection = lookup("#todaySection").query();
        assertFalse(todaySection.isVisible(), "Today section should be hidden when no tasks");
    }
    
    @Test
    @DisplayName("Empty state should hide when tasks are loaded")
    void testEmptyStateHidesWhenTasksLoaded() throws Exception {
        // Given: initially no tasks
        CountDownLatch latch1 = new CountDownLatch(1);
        interact(() -> {
            controller.loadTasks();
            latch1.countDown();
        });
        assertTrue(latch1.await(5, TimeUnit.SECONDS));
        
        VBox emptyStateContainer = lookup("#emptyStateContainer").query();
        assertTrue(emptyStateContainer.isVisible(), "Empty state should be visible initially");
        
        // When: a task is added
        LocalDate today = LocalDate.now();
        taskRepository.save(Task.builder()
            .title("New Task")
            .status(TaskStatus.TODO)
            .scheduledDate(today)
            .dueDate(today)
            .priority(Priority.MEDIUM)
            .build());
        
        // And: view is refreshed
        CountDownLatch latch2 = new CountDownLatch(1);
        interact(() -> {
            controller.loadTasks();
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        
        // Then: empty state should be hidden
        assertFalse(emptyStateContainer.isVisible(), "Empty state should be hidden when tasks exist");
        
        // And: today section should be visible
        VBox todaySection = lookup("#todaySection").query();
        assertTrue(todaySection.isVisible(), "Today section should be visible when tasks exist");
    }
    
    @Test
    @DisplayName("Completed tasks should not be shown in Today view")
    void testCompletedTasksNotShown() throws Exception {
        // Given: a completed task for today
        LocalDate today = LocalDate.now();
        taskRepository.save(Task.builder()
            .title("Completed Task")
            .status(TaskStatus.COMPLETED)
            .scheduledDate(today)
            .dueDate(today)
            .priority(Priority.MEDIUM)
            .build());
        
        // When: viewing Today view
        CountDownLatch latch = new CountDownLatch(1);
        interact(() -> {
            controller.loadTasks();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Then: empty state should still be shown
        VBox emptyStateContainer = lookup("#emptyStateContainer").query();
        assertTrue(emptyStateContainer.isVisible(), 
            "Empty state should be visible when only completed tasks exist");
    }
    
    @Test
    @DisplayName("Task counts should be displayed correctly")
    void testTaskCounts() throws Exception {
        // Given: multiple overdue and today's tasks
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // Create 2 overdue tasks
        for (int i = 0; i < 2; i++) {
            taskRepository.save(Task.builder()
                .title("Overdue Task " + i)
                .status(TaskStatus.TODO)
                .dueDate(yesterday)
                .priority(Priority.MEDIUM)
                .build());
        }
        
        // Create 3 today's tasks
        for (int i = 0; i < 3; i++) {
            taskRepository.save(Task.builder()
                .title("Today Task " + i)
                .status(TaskStatus.TODO)
                .scheduledDate(today)
                .dueDate(today)
                .priority(Priority.MEDIUM)
                .build());
        }
        
        // When: viewing Today view
        CountDownLatch latch = new CountDownLatch(1);
        interact(() -> {
            controller.loadTasks();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Then: overdue count should show 2
        javafx.scene.control.Label overdueCount = lookup("#overdueCount").query();
        assertEquals("2", overdueCount.getText(), "Overdue count should be 2");
        
        // And: today count should show 3
        javafx.scene.control.Label todayCount = lookup("#todayCount").query();
        assertEquals("3", todayCount.getText(), "Today count should be 3");
    }
}
