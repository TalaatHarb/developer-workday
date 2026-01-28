package net.talaatharb.workday.utils;

import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

/**
 * Manager for undo/redo functionality using the Command pattern
 */
@Slf4j
public class UndoRedoManager {
    
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();
    private final int maxHistorySize;
    
    public UndoRedoManager() {
        this(100); // Default max 100 commands in history
    }
    
    public UndoRedoManager(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Execute a command and add it to the undo stack
     */
    public void execute(Command command) {
        log.debug("Executing command: {}", command.getDescription());
        
        command.execute();
        undoStack.push(command);
        
        // Clear redo stack when a new command is executed
        redoStack.clear();
        
        // Limit history size
        if (undoStack.size() > maxHistorySize) {
            undoStack.remove(0);
        }
        
        log.info("Command executed: {}", command.getDescription());
    }
    
    /**
     * Undo the last command
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            log.warn("No commands to undo");
            return false;
        }
        
        Command command = undoStack.pop();
        log.debug("Undoing command: {}", command.getDescription());
        
        command.undo();
        redoStack.push(command);
        
        log.info("Command undone: {}", command.getDescription());
        return true;
    }
    
    /**
     * Redo the last undone command
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            log.warn("No commands to redo");
            return false;
        }
        
        Command command = redoStack.pop();
        log.debug("Redoing command: {}", command.getDescription());
        
        command.execute();
        undoStack.push(command);
        
        log.info("Command redone: {}", command.getDescription());
        return true;
    }
    
    /**
     * Check if undo is available
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Check if redo is available
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Get description of the next undoable command
     */
    public String getUndoDescription() {
        if (undoStack.isEmpty()) {
            return null;
        }
        return undoStack.peek().getDescription();
    }
    
    /**
     * Get description of the next redoable command
     */
    public String getRedoDescription() {
        if (redoStack.isEmpty()) {
            return null;
        }
        return redoStack.peek().getDescription();
    }
    
    /**
     * Clear all undo/redo history
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        log.info("Undo/redo history cleared");
    }
    
    /**
     * Get size of undo stack
     */
    public int getUndoStackSize() {
        return undoStack.size();
    }
    
    /**
     * Get size of redo stack
     */
    public int getRedoStackSize() {
        return redoStack.size();
    }
}
