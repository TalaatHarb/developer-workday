package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.facade.CalendarFacade;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Task;

/**
 * Calendar view controller backed by CalendarFX.
 * CalendarFX provides built-in month/week/day navigation and a polished calendar UI.
 */
@Slf4j
public class CalendarViewController implements Initializable {

    @FXML
    private VBox calendarContainer;

    @Setter
    private TaskFacade taskFacade;

    @Setter
    private CalendarFacade calendarFacade;

    @Setter
    private EventDispatcher eventDispatcher;

    private CalendarView calendarFxView;
    private Calendar taskCalendar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing CalendarViewController with CalendarFX");

        taskCalendar = new Calendar("Tasks");
        taskCalendar.setStyle(Calendar.Style.STYLE1);
        taskCalendar.setReadOnly(false);

        CalendarSource source = new CalendarSource("Developer Workday");
        source.getCalendars().add(taskCalendar);

        calendarFxView = new CalendarView();
        calendarFxView.getCalendarSources().setAll(source);
        calendarFxView.setRequestedTime(LocalTime.now());
        VBox.setVgrow(calendarFxView, Priority.ALWAYS);
        calendarContainer.getChildren().add(calendarFxView);

        // Daemon thread to keep CalendarFX's "now" marker accurate
        Thread timeThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Platform.runLater(() -> calendarFxView.setRequestedTime(LocalTime.now()));
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        timeThread.setDaemon(true);
        timeThread.start();

        log.info("CalendarViewController initialized");
    }

    /**
     * Called by MainUiController after dependency injection to populate the calendar.
     */
    public void loadCalendarTasks() {
        if (calendarFacade == null) {
            log.warn("CalendarFacade not set, skipping task load");
            return;
        }

        LocalDate start = LocalDate.now().minusMonths(6);
        LocalDate end = LocalDate.now().plusMonths(12);

        try {
            List<Task> tasks = calendarFacade.getTasksForPeriod(start, end);
            log.debug("Loading {} tasks into CalendarFX", tasks.size());
            Platform.runLater(() -> {
                taskCalendar.clear();
                for (Task task : tasks) {
                    addTaskToCalendar(task);
                }
            });
        } catch (Exception e) {
            log.error("Failed to load calendar tasks", e);
        }
    }

    private void addTaskToCalendar(Task task) {
        LocalDate date = task.getScheduledDate() != null ? task.getScheduledDate() : task.getDueDate();
        if (date == null) return;

        String title = task.getTitle() != null ? task.getTitle() : "(Untitled)";
        Entry<Task> entry = new Entry<>(title);

        if (task.getDueTime() != null) {
            LocalTime endTime = task.getDueTime().plusMinutes(30);
            entry.setInterval(date, task.getDueTime(), date, endTime);
        } else {
            entry.setInterval(date);
            entry.setFullDay(true);
        }

        entry.setUserObject(task);
        taskCalendar.addEntry(entry);
    }

    /**
     * Show tasks for a specific day in an info dialog.
     * Can be called programmatically (e.g. from tests or other controllers).
     */
    void handleDayClick(LocalDate date) {
        log.debug("Day clicked: {}", date);

        List<Task> tasksForDay;
        if (calendarFacade != null) {
            try {
                tasksForDay = calendarFacade.getTasksForDay(date);
            } catch (Exception e) {
                log.error("Failed to fetch tasks for day {}", date, e);
                tasksForDay = List.of();
            }
        } else {
            tasksForDay = List.of();
        }

        javafx.scene.control.Alert dialog = createDayTasksDialog(date, tasksForDay);
        dialog.showAndWait();
    }

    /**
     * Visible-for-testing factory for the day-tasks info dialog.
     */
    javafx.scene.control.Alert createDayTasksDialog(LocalDate date, List<Task> tasksForDay) {
        javafx.scene.control.Alert dialog = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        dialog.setTitle("Tasks");
        dialog.setHeaderText("Tasks on " + date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        if (tasksForDay.isEmpty()) {
            dialog.setContentText("No tasks scheduled.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Task t : tasksForDay) {
                sb.append("• ").append(t.getTitle() == null ? "(untitled)" : t.getTitle());
                if (t.getDueTime() != null) {
                    sb.append("  @ ").append(t.getDueTime());
                }
                sb.append('\n');
            }
            dialog.setContentText(sb.toString());
        }
        return dialog;
    }
}
