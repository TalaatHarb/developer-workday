package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.task.TaskCreatedEvent;
import net.talaatharb.workday.facade.CategoryFacade;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Task;

/**
 * Controller for quick add task functionality.
 * Handles natural language parsing and real-time highlighting.
 */
@Slf4j
public class QuickAddController implements Initializable {
    
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(today|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday|next\\s+\\w+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile("\\bat\\s+(\\d{1,2})(:(\\d{2}))?(\\s*([ap]m))?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_PATTERN = Pattern.compile("#(\\w+)");
    private static final Pattern PRIORITY_PATTERN = Pattern.compile("!(urgent|high|medium|low)", Pattern.CASE_INSENSITIVE);
    
    @FXML
    private TextField quickAddField;
    
    @Setter
    private TaskFacade taskFacade;
    
    @Setter
    private CategoryFacade categoryFacade;
    
    @Setter
    private EventDispatcher eventDispatcher;
    
    private ContextMenu autocompleteMenu;
    private List<Category> availableCategories = new ArrayList<>();
    private Runnable onTaskCreatedCallback;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing QuickAddController");
        
        // Setup autocomplete menu
        autocompleteMenu = new ContextMenu();
        
        // Add text change listener for real-time parsing and highlighting
        quickAddField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                handleTextChange(newValue);
            }
        });
        
        // Add key press handler for Enter and autocomplete triggers
        quickAddField.setOnKeyPressed(this::handleKeyPress);
        
        log.info("QuickAddController initialized successfully");
    }
    
    /**
     * Handle text changes for real-time highlighting and autocomplete
     */
    private void handleTextChange(String text) {
        if (text == null || text.isEmpty()) {
            autocompleteMenu.hide();
            return;
        }
        
        // Check for autocomplete triggers
        int caretPosition = quickAddField.getCaretPosition();
        if (caretPosition > 0) {
            char prevChar = text.charAt(caretPosition - 1);
            
            if (prevChar == '#') {
                showCategoryAutocomplete();
            } else if (prevChar == '@') {
                // TODO: Show contact autocomplete when contacts are implemented
                log.debug("Contact autocomplete triggered (not yet implemented)");
            } else {
                // Update autocomplete if already showing
                if (autocompleteMenu.isShowing()) {
                    String currentWord = getCurrentWord(text, caretPosition);
                    if (currentWord.startsWith("#")) {
                        filterCategoryAutocomplete(currentWord.substring(1));
                    }
                }
            }
        }
        
        // Apply syntax highlighting (visual feedback would require custom TextField)
        // For now, we'll log the parsed elements
        logParsedElements(text);
    }
    
    /**
     * Handle key press events
     */
    @FXML
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleQuickAddSubmit();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            autocompleteMenu.hide();
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN && autocompleteMenu.isShowing()) {
            // Let autocomplete menu handle navigation
            event.consume();
        }
    }
    
    /**
     * Handle quick add submission
     */
    @FXML
    public void handleQuickAddSubmit() {
        String input = quickAddField.getText();
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        
        log.info("Quick adding task: {}", input);
        
        try {
            // Use facade to parse and create task
            if (taskFacade != null) {
                Task createdTask = taskFacade.quickAddTask(input);
                
                // Publish event if dispatcher is available
                if (eventDispatcher != null) {
                    eventDispatcher.publish(new TaskCreatedEvent(createdTask));
                }
                
                // Clear input field
                quickAddField.clear();
                
                // Trigger callback if set
                if (onTaskCreatedCallback != null) {
                    onTaskCreatedCallback.run();
                }
                
                log.info("Successfully created task: {}", createdTask.getTitle());
            } else {
                log.warn("TaskFacade not set, cannot create task");
            }
        } catch (Exception e) {
            log.error("Failed to create task from quick add", e);
            // TODO: Show error notification to user
        }
    }
    
    /**
     * Show category autocomplete suggestions
     */
    private void showCategoryAutocomplete() {
        if (categoryFacade == null) {
            log.debug("CategoryFacade not set, cannot show autocomplete");
            return;
        }
        
        // Load categories if not already loaded
        if (availableCategories.isEmpty()) {
            loadCategories();
        }
        
        // Build menu items
        autocompleteMenu.getItems().clear();
        for (Category category : availableCategories) {
            MenuItem item = new MenuItem(category.getName());
            item.setOnAction(e -> insertAutocomplete("#" + category.getName()));
            autocompleteMenu.getItems().add(item);
        }
        
        // Show menu below the text field
        if (!autocompleteMenu.getItems().isEmpty()) {
            autocompleteMenu.show(quickAddField, Side.BOTTOM, 0, 0);
        }
    }
    
    /**
     * Filter category autocomplete based on current input
     */
    private void filterCategoryAutocomplete(String filter) {
        autocompleteMenu.getItems().clear();
        
        String lowerFilter = filter.toLowerCase();
        for (Category category : availableCategories) {
            if (category.getName().toLowerCase().startsWith(lowerFilter)) {
                MenuItem item = new MenuItem(category.getName());
                item.setOnAction(e -> insertAutocomplete("#" + category.getName()));
                autocompleteMenu.getItems().add(item);
            }
        }
        
        if (autocompleteMenu.getItems().isEmpty()) {
            autocompleteMenu.hide();
        }
    }
    
    /**
     * Insert autocomplete selection at caret position
     */
    private void insertAutocomplete(String text) {
        int caretPos = quickAddField.getCaretPosition();
        String currentText = quickAddField.getText();
        
        // Find start of current word (after # or @)
        int wordStart = caretPos - 1;
        while (wordStart > 0 && currentText.charAt(wordStart - 1) != ' ' && currentText.charAt(wordStart - 1) != '#' && currentText.charAt(wordStart - 1) != '@') {
            wordStart--;
        }
        
        // Replace from # to caret with autocomplete text
        String newText = currentText.substring(0, wordStart) + text + " " + currentText.substring(caretPos);
        quickAddField.setText(newText);
        quickAddField.positionCaret(wordStart + text.length() + 1);
        
        autocompleteMenu.hide();
    }
    
    /**
     * Get the current word at caret position
     */
    private String getCurrentWord(String text, int caretPosition) {
        int start = caretPosition - 1;
        while (start >= 0 && text.charAt(start) != ' ') {
            start--;
        }
        start++; // Move past the space
        
        return text.substring(start, caretPosition);
    }
    
    /**
     * Load categories from facade
     */
    private void loadCategories() {
        if (categoryFacade != null) {
            try {
                availableCategories = categoryFacade.findAll();
                log.debug("Loaded {} categories for autocomplete", availableCategories.size());
            } catch (Exception e) {
                log.error("Failed to load categories", e);
                availableCategories = new ArrayList<>();
            }
        }
    }
    
    /**
     * Log parsed elements (for debugging and to show parsing works)
     */
    private void logParsedElements(String text) {
        // Find all patterns
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        Matcher tagMatcher = TAG_PATTERN.matcher(text);
        Matcher priorityMatcher = PRIORITY_PATTERN.matcher(text);
        
        List<String> elements = new ArrayList<>();
        
        while (dateMatcher.find()) {
            elements.add("date:" + dateMatcher.group());
        }
        
        while (timeMatcher.find()) {
            elements.add("time:" + timeMatcher.group());
        }
        
        while (tagMatcher.find()) {
            elements.add("tag:" + tagMatcher.group());
        }
        
        while (priorityMatcher.find()) {
            elements.add("priority:" + priorityMatcher.group());
        }
        
        if (!elements.isEmpty()) {
            log.trace("Parsed elements: {}", String.join(", ", elements));
        }
    }
    
    /**
     * Set callback to execute when a task is created
     */
    public void setOnTaskCreatedCallback(Runnable callback) {
        this.onTaskCreatedCallback = callback;
    }
    
    /**
     * Get the quick add text field for external access
     */
    public TextField getQuickAddField() {
        return quickAddField;
    }
    
    /**
     * Set focus to the quick add field
     */
    public void focus() {
        Platform.runLater(() -> quickAddField.requestFocus());
    }
}
