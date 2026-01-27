package net.talaatharb.workday.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for handling errors gracefully in the application.
 * Provides user-friendly error dialogs and comprehensive logging.
 */
@Slf4j
public class ErrorHandler {
    
    /**
     * Handle an exception with a user-friendly dialog and logging
     * 
     * @param title the dialog title
     * @param message the user-friendly message
     * @param throwable the exception that occurred
     */
    public static void handleError(String title, String message, Throwable throwable) {
        log.error("{}: {}", title, message, throwable);
        
        // Show dialog on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            showErrorDialog(title, message, throwable);
        } else {
            Platform.runLater(() -> showErrorDialog(title, message, throwable));
        }
    }
    
    /**
     * Handle an exception with logging only (no dialog)
     * 
     * @param message the log message
     * @param throwable the exception that occurred
     */
    public static void logError(String message, Throwable throwable) {
        log.error(message, throwable);
    }
    
    /**
     * Handle a warning with a user-friendly dialog and logging
     * 
     * @param title the dialog title
     * @param message the warning message
     */
    public static void handleWarning(String title, String message) {
        log.warn("{}: {}", title, message);
        
        // Show dialog on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            showWarningDialog(title, message);
        } else {
            Platform.runLater(() -> showWarningDialog(title, message));
        }
    }
    
    /**
     * Show an error dialog with expandable stack trace
     */
    private static void showErrorDialog(String title, String message, Throwable throwable) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(getShortErrorMessage(throwable));
        
        // Create expandable Exception.
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String exceptionText = sw.toString();
            
            Label label = new Label("Stack trace:");
            
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);
            
            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);
            
            alert.getDialogPane().setExpandableContent(expContent);
        }
        
        alert.showAndWait();
    }
    
    /**
     * Show a warning dialog
     */
    private static void showWarningDialog(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Get a short, user-friendly error message from a throwable
     */
    private static String getShortErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "An unknown error occurred.";
        }
        
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            message = throwable.getClass().getSimpleName();
        }
        
        // Truncate if too long
        if (message.length() > 200) {
            message = message.substring(0, 197) + "...";
        }
        
        return message;
    }
    
    /**
     * Handle a database error specifically
     */
    public static void handleDatabaseError(Throwable throwable) {
        handleError(
            "Database Error",
            "An error occurred while accessing the database. Your data may not have been saved.",
            throwable
        );
    }
    
    /**
     * Handle a file I/O error
     */
    public static void handleFileError(String operation, Throwable throwable) {
        handleError(
            "File Operation Error",
            String.format("An error occurred while %s. Please check file permissions and try again.", operation),
            throwable
        );
    }
    
    /**
     * Handle a validation error
     */
    public static void handleValidationError(String message) {
        handleWarning("Validation Error", message);
    }
}
