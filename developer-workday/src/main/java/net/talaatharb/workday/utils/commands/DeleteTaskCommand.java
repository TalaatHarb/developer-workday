package net.talaatharb.workday.utils.commands;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.service.TaskService;
import net.talaatharb.workday.utils.Command;

/**
 * Command for deleting a task (undoable)
 */
@Slf4j
@RequiredArgsConstructor
public class DeleteTaskCommand implements Command {
    
    private final TaskService taskService;
    private final UUID taskId;
    private Task deletedTask;
    
    @Override
    public void execute() {
        // Save the task before deleting so we can restore it
        deletedTask = taskService.findById(taskId).orElse(null);
        if (deletedTask != null) {
            taskService.deleteTask(taskId);
            log.info("Task deleted: {}", deletedTask.getTitle());
        }
    }
    
    @Override
    public void undo() {
        if (deletedTask != null) {
            taskService.createTask(deletedTask);
            log.info("Task delete undone: {}", deletedTask.getTitle());
        }
    }
    
    @Override
    public String getDescription() {
        return deletedTask != null ? "Delete task: " + deletedTask.getTitle() : "Delete task";
    }
}
