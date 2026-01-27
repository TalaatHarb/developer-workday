package net.talaatharb.workday.utils;

/**
 * Interface for commands that can be undone and redone
 */
public interface Command {
    
    /**
     * Execute the command
     */
    void execute();
    
    /**
     * Undo the command
     */
    void undo();
    
    /**
     * Get a description of this command for display purposes
     */
    String getDescription();
}
