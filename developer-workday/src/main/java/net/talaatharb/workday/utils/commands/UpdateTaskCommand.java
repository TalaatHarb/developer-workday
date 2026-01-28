package net.talaatharb.workday.utils.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.service.TaskService;
import net.talaatharb.workday.utils.Command;

/**
 * Command for updating a task (undoable)
 */
@Slf4j
@RequiredArgsConstructor
public class UpdateTaskCommand implements Command {
    
    private final TaskService taskService;
    private final Task oldTask;
    private final Task newTask;
    
    @Override
    public void execute() {
        taskService.updateTask(newTask);
        log.info("Task updated: {}", newTask.getTitle());
    }
    
    @Override
    public void undo() {
        taskService.updateTask(oldTask);
        log.info("Task update undone: {}", oldTask.getTitle());
    }
    
    @Override
    public String getDescription() {
        return "Update task: " + newTask.getTitle();
    }
}
