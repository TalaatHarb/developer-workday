package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.category.*;
import net.talaatharb.workday.facade.CategoryFacade;
import net.talaatharb.workday.model.Category;

/**
 * Controller for category management dialog.
 */
@Slf4j
public class CategoryManagementDialogController implements Initializable {
    
    @FXML private ListView<Category> categoryListView;
    @FXML private Button addButton, editButton, deleteButton, saveButton, cancelButton, closeButton;
    @FXML private VBox editorForm;
    @FXML private Label formTitleLabel;
    @FXML private TextField nameField, iconField;
    @FXML private ColorPicker colorPicker;
    
    @Setter private CategoryFacade categoryFacade;
    @Setter private EventDispatcher eventDispatcher;
    private Category editingCategory;
    private Runnable onCloseCallback;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing CategoryManagementDialogController");
        
        // Setup list view
        categoryListView.setCellFactory(lv -> new CategoryCell());
        categoryListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                boolean selected = newVal != null;
                editButton.setDisable(!selected);
                deleteButton.setDisable(!selected);
            });
        
        // Setup drag and drop for reordering
        setupDragAndDrop();
        
        loadCategories();
        log.info("CategoryManagementDialogController initialized");
    }
    
    private void loadCategories() {
        if (categoryFacade != null) {
            try {
                List<Category> categories = categoryFacade.findAll();
                categoryListView.setItems(FXCollections.observableArrayList(categories));
            } catch (Exception e) {
                log.error("Failed to load categories", e);
            }
        }
    }
    
    @FXML
    private void handleAdd() {
        showEditor(null);
    }
    
    @FXML
    private void handleEdit() {
        Category selected = categoryListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showEditor(selected);
        }
    }
    
    @FXML
    private void handleDelete() {
        Category selected = categoryListView.getSelectionModel().getSelectedItem();
        if (selected != null && categoryFacade != null) {
            // TODO: Add confirmation dialog
            categoryFacade.deleteCategory(selected.getId());
            if (eventDispatcher != null) {
                eventDispatcher.publish(new CategoryDeletedEvent(selected.getId(), selected.getName()));
            }
            loadCategories();
        }
    }
    
    @FXML
    private void handleSave() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            log.warn("Category name is required");
            return;
        }
        
        try {
            if (editingCategory == null) {
                // Create new
                Category category = Category.builder()
                    .name(name)
                    .color(toHexColor(colorPicker.getValue()))
                    .icon(iconField.getText())
                    .displayOrder(categoryListView.getItems().size())
                    .build();
                
                Category created = categoryFacade.createCategory(category);
                if (eventDispatcher != null) {
                    eventDispatcher.publish(new CategoryCreatedEvent(created));
                }
            } else {
                // Update existing
                Category updated = Category.builder()
                    .id(editingCategory.getId())
                    .name(name)
                    .color(toHexColor(colorPicker.getValue()))
                    .icon(iconField.getText())
                    .displayOrder(editingCategory.getDisplayOrder())
                    .createdAt(editingCategory.getCreatedAt())
                    .build();
                
                categoryFacade.updateCategory(updated);
                if (eventDispatcher != null) {
                    eventDispatcher.publish(new CategoryUpdatedEvent(editingCategory, updated));
                }
            }
            
            hideEditor();
            loadCategories();
        } catch (Exception e) {
            log.error("Failed to save category", e);
        }
    }
    
    @FXML
    private void handleCancel() {
        hideEditor();
    }
    
    @FXML
    private void handleClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
    
    private void showEditor(Category category) {
        editingCategory = category;
        formTitleLabel.setText(category == null ? "New Category" : "Edit Category");
        nameField.setText(category != null ? category.getName() : "");
        iconField.setText(category != null ? category.getIcon() : "");
        colorPicker.setValue(category != null ? Color.web(category.getColor()) : Color.BLUE);
        
        editorForm.setVisible(true);
        editorForm.setManaged(true);
    }
    
    private void hideEditor() {
        editorForm.setVisible(false);
        editorForm.setManaged(false);
        editingCategory = null;
    }
    
    private void setupDragAndDrop() {
        categoryListView.setCellFactory(lv -> {
            CategoryCell cell = new CategoryCell();
            
            cell.setOnDragDetected(event -> {
                if (cell.getItem() != null) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(cell.getItem().getId().toString());
                    db.setContent(content);
                    event.consume();
                }
            });
            
            cell.setOnDragOver(event -> {
                if (event.getDragboard().hasString() && cell.getItem() != null) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });
            
            cell.setOnDragDropped(event -> {
                // Handle reorder logic
                log.debug("Category reordered");
                event.setDropCompleted(true);
                event.consume();
            });
            
            return cell;
        });
    }
    
    private String toHexColor(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    private static class CategoryCell extends ListCell<Category> {
        @Override
        protected void updateItem(Category item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String icon = item.getIcon() != null ? item.getIcon() + " " : "";
                setText(icon + item.getName());
                setStyle("-fx-background-color: " + item.getColor() + "20;");
            }
        }
    }
}
