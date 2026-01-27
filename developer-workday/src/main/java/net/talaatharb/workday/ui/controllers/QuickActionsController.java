package net.talaatharb.workday.ui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.QuickAction;
import net.talaatharb.workday.service.QuickActionsService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Quick Actions dialog (command palette).
 */
@Slf4j
public class QuickActionsController implements Initializable {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ListView<QuickAction> actionsListView;
    
    @FXML
    private Label infoLabel;
    
    @FXML
    private Label statusLabel;
    
    @Setter
    private QuickActionsService quickActionsService;
    
    private Stage dialogStage;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing QuickActionsController");
        
        // Setup list view custom cell factory
        actionsListView.setCellFactory(lv -> new QuickActionCell());
        
        // Setup search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterActions(newVal);
        });
        
        // Setup keyboard navigation
        searchField.setOnKeyPressed(this::handleKeyPress);
        actionsListView.setOnKeyPressed(this::handleKeyPress);
        
        log.info("QuickActionsController initialized");
    }
    
    /**
     * Load actions from the service.
     */
    public void loadActions() {
        if (quickActionsService == null) {
            log.warn("QuickActionsService not set");
            return;
        }
        
        List<QuickAction> actions = quickActionsService.getAllActions();
        actionsListView.getItems().setAll(actions);
        
        if (!actions.isEmpty()) {
            actionsListView.getSelectionModel().selectFirst();
        }
        
        updateStatusLabel();
        
        // Focus search field
        Platform.runLater(() -> searchField.requestFocus());
    }
    
    /**
     * Filter actions based on search query.
     */
    private void filterActions(String query) {
        if (quickActionsService == null) {
            return;
        }
        
        List<QuickAction> filteredActions = quickActionsService.searchActions(query);
        actionsListView.getItems().setAll(filteredActions);
        
        if (!filteredActions.isEmpty()) {
            actionsListView.getSelectionModel().selectFirst();
        }
        
        updateStatusLabel();
    }
    
    /**
     * Handle keyboard events.
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            closeDialog();
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER) {
            executeSelectedAction();
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            if (event.getSource() == searchField) {
                actionsListView.requestFocus();
                event.consume();
            }
        } else if (event.getCode() == KeyCode.UP) {
            if (actionsListView.getSelectionModel().getSelectedIndex() == 0) {
                searchField.requestFocus();
                event.consume();
            }
        }
    }
    
    /**
     * Execute the selected action.
     */
    private void executeSelectedAction() {
        QuickAction selectedAction = actionsListView.getSelectionModel().getSelectedItem();
        if (selectedAction != null && quickActionsService != null) {
            closeDialog();
            
            // Execute on Platform thread after a short delay to allow dialog to close
            Platform.runLater(() -> {
                try {
                    quickActionsService.executeAction(selectedAction);
                } catch (Exception e) {
                    log.error("Failed to execute action: {}", selectedAction.getTitle(), e);
                }
            });
        }
    }
    
    /**
     * Update status label with action count.
     */
    private void updateStatusLabel() {
        int count = actionsListView.getItems().size();
        statusLabel.setText(count + " action" + (count != 1 ? "s" : "") + " available");
    }
    
    /**
     * Set the dialog stage.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    /**
     * Close the dialog.
     */
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    /**
     * Custom cell for displaying quick actions.
     */
    private static class QuickActionCell extends ListCell<QuickAction> {
        @Override
        protected void updateItem(QuickAction action, boolean empty) {
            super.updateItem(action, empty);
            
            if (empty || action == null) {
                setText(null);
                setGraphic(null);
            } else {
                StringBuilder text = new StringBuilder();
                text.append(action.getTitle());
                
                if (action.getCategory() != null && !action.getCategory().isEmpty()) {
                    text.append(" • ").append(action.getCategory());
                }
                
                if (action.getDescription() != null && !action.getDescription().isEmpty()) {
                    text.append("\n  ").append(action.getDescription());
                }
                
                setText(text.toString());
                setStyle("-fx-padding: 8 12 8 12;");
            }
        }
    }
}
