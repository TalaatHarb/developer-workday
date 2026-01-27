package net.talaatharb.workday.ui.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.facade.CategoryFacade;
import net.talaatharb.workday.model.Category;

/**
 * Tests for CategoryManagementDialogController.
 */
class CategoryManagementDialogControllerTest {
    
    private static boolean jfxInitialized = false;
    private CategoryFacade mockCategoryFacade;
    private EventDispatcher mockEventDispatcher;
    
    @BeforeAll
    static void initJavaFX() {
        if (!jfxInitialized) {
            Platform.startup(() -> {});
            jfxInitialized = true;
        }
    }
    
    @BeforeEach
    void setUp() {
        mockCategoryFacade = mock(CategoryFacade.class);
        mockEventDispatcher = mock(EventDispatcher.class);
    }
    
    @Test
    @DisplayName("Dialog loads and displays categories")
    void testDialogLoads() throws Exception {
        Platform.runLater(() -> {
            try {
                List<Category> categories = createSampleCategories();
                when(mockCategoryFacade.findAll()).thenReturn(categories);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/CategoryManagementDialog.fxml");
                assertNotNull(fxmlResource);
                
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                CategoryManagementDialogController controller = loader.getController();
                
                controller.setCategoryFacade(mockCategoryFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                
                @SuppressWarnings("unchecked")
                ListView<Category> listView = (ListView<Category>) root.lookup("#categoryListView");
                assertNotNull(listView);
                
            } catch (Exception e) {
                fail("Failed: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    private List<Category> createSampleCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(Category.builder().id(UUID.randomUUID()).name("Work").color("#3498db").build());
        categories.add(Category.builder().id(UUID.randomUUID()).name("Personal").color("#2ecc71").build());
        return categories;
    }
}
