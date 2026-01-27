package net.talaatharb.workday.utils;

import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class DragAndDropHelperTest {
    
    private ListView<String> listView;
    private DragAndDropHelper<String> dragAndDropHelper;
    private AtomicReference<DragAndDropHelper.DragResult<String>> lastResult;
    
    @Start
    void start(Stage stage) {
        listView = new ListView<>();
        listView.getItems().addAll("Task 1", "Task 2", "Task 3", "Task 4");
        
        lastResult = new AtomicReference<>();
        dragAndDropHelper = new DragAndDropHelper<>(listView);
        dragAndDropHelper.enableDragAndDrop(result -> {
            lastResult.set(result);
        });
        
        VBox root = new VBox(listView);
        Scene scene = new Scene(root, 300, 400);
        stage.setScene(scene);
        stage.show();
    }
    
    @Test
    void shouldInitializeDragAndDropHelper() {
        assertNotNull(dragAndDropHelper);
        assertNotNull(listView.getCellFactory());
    }
    
    @Test
    void shouldHaveCorrectInitialItems() {
        assertEquals(4, listView.getItems().size());
        assertEquals("Task 1", listView.getItems().get(0));
        assertEquals("Task 2", listView.getItems().get(1));
        assertEquals("Task 3", listView.getItems().get(2));
        assertEquals("Task 4", listView.getItems().get(3));
    }
    
    @Test
    void dragResultShouldContainCorrectInformation() {
        // Create a drag result manually for testing
        DragAndDropHelper.DragResult<String> result = 
            new DragAndDropHelper.DragResult<>("Test Item", 0, 2);
        
        assertEquals("Test Item", result.getItem());
        assertEquals(0, result.getFromIndex());
        assertEquals(2, result.getToIndex());
        assertTrue(result.isReordered());
    }
    
    @Test
    void dragResultShouldDetectNoReorder() {
        DragAndDropHelper.DragResult<String> result = 
            new DragAndDropHelper.DragResult<>("Test Item", 1, 1);
        
        assertFalse(result.isReordered());
    }
    
    @Test
    void dragResultToStringShouldBeInformative() {
        DragAndDropHelper.DragResult<String> result = 
            new DragAndDropHelper.DragResult<>("My Task", 0, 3);
        
        String str = result.toString();
        assertTrue(str.contains("My Task"));
        assertTrue(str.contains("from=0"));
        assertTrue(str.contains("to=3"));
    }
    
    @Test
    void shouldAllowManualReordering() {
        // Simulate manual reordering (what drag-drop would do)
        String item = listView.getItems().remove(0);
        listView.getItems().add(2, item);
        
        // Verify new order
        assertEquals("Task 2", listView.getItems().get(0));
        assertEquals("Task 3", listView.getItems().get(1));
        assertEquals("Task 1", listView.getItems().get(2));
        assertEquals("Task 4", listView.getItems().get(3));
    }
    
    @Test
    void shouldHandleEmptyList() {
        ListView<String> emptyList = new ListView<>();
        DragAndDropHelper<String> helper = new DragAndDropHelper<>(emptyList);
        
        assertDoesNotThrow(() -> {
            helper.enableDragAndDrop(result -> {});
        });
        
        assertEquals(0, emptyList.getItems().size());
    }
    
    @Test
    void shouldHandleSingleItemList() {
        ListView<String> singleItemList = new ListView<>();
        singleItemList.getItems().add("Only Task");
        
        DragAndDropHelper<String> helper = new DragAndDropHelper<>(singleItemList);
        helper.enableDragAndDrop(result -> {});
        
        assertEquals(1, singleItemList.getItems().size());
        assertEquals("Only Task", singleItemList.getItems().get(0));
    }
}
