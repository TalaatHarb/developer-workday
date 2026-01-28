package net.talaatharb.workday.utils;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Utility class to enable drag-and-drop reordering in JavaFX ListViews.
 * Provides a simple API to make any ListView items draggable and reorderable.
 * 
 * @param <T> The type of items in the ListView
 */
@Slf4j
public class DragAndDropHelper<T> {
    
    private static final String DRAG_KEY = "listitem";
    private final ListView<T> listView;
    private Consumer<DragResult<T>> onReorder;
    
    public DragAndDropHelper(ListView<T> listView) {
        this.listView = listView;
    }
    
    /**
     * Enable drag-and-drop reordering for the ListView.
     * @param onReorder Callback invoked when items are reordered
     */
    public void enableDragAndDrop(Consumer<DragResult<T>> onReorder) {
        this.onReorder = onReorder;
        
        listView.setCellFactory(lv -> {
            ListCell<T> cell = new ListCell<T>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.toString());
                    }
                }
            };
            
            setupDragHandlers(cell);
            return cell;
        });
    }
    
    /**
     * Enable drag-and-drop with a custom cell factory.
     * @param cellFactory Custom cell factory for rendering items
     * @param onReorder Callback invoked when items are reordered
     */
    public void enableDragAndDrop(
            javafx.util.Callback<ListView<T>, ListCell<T>> cellFactory,
            Consumer<DragResult<T>> onReorder) {
        this.onReorder = onReorder;
        
        listView.setCellFactory(lv -> {
            ListCell<T> cell = cellFactory.call(lv);
            setupDragHandlers(cell);
            return cell;
        });
    }
    
    private void setupDragHandlers(ListCell<T> cell) {
        // Handle drag detected
        cell.setOnDragDetected(event -> {
            if (cell.getItem() == null) {
                return;
            }
            
            Dragboard dragboard = cell.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(DRAG_KEY);
            dragboard.setContent(content);
            
            log.debug("Drag started for item: {}", cell.getItem());
            event.consume();
        });
        
        // Handle drag over
        cell.setOnDragOver(event -> {
            if (event.getDragboard().hasString() && 
                DRAG_KEY.equals(event.getDragboard().getString())) {
                event.acceptTransferModes(TransferMode.MOVE);
                
                // Visual feedback
                if (!cell.isEmpty()) {
                    cell.setStyle("-fx-background-color: rgba(0, 122, 204, 0.2);");
                }
            }
            event.consume();
        });
        
        // Handle drag exited (clear visual feedback)
        cell.setOnDragExited(event -> {
            cell.setStyle("");
            event.consume();
        });
        
        // Handle drop
        cell.setOnDragDropped(event -> {
            if (event.getDragboard().hasString() &&
                DRAG_KEY.equals(event.getDragboard().getString())) {
                
                ListCell<T> sourceCell = (ListCell<T>) event.getGestureSource();
                T draggedItem = sourceCell.getItem();
                T targetItem = cell.getItem();
                
                if (draggedItem != null && draggedItem != targetItem) {
                    int sourceIndex = listView.getItems().indexOf(draggedItem);
                    int targetIndex = cell.isEmpty() ? 
                        listView.getItems().size() - 1 : 
                        listView.getItems().indexOf(targetItem);
                    
                    // Reorder items
                    listView.getItems().remove(sourceIndex);
                    listView.getItems().add(targetIndex, draggedItem);
                    
                    // Notify callback
                    if (onReorder != null) {
                        DragResult<T> result = new DragResult<>(
                            draggedItem, sourceIndex, targetIndex
                        );
                        onReorder.accept(result);
                    }
                    
                    log.info("Item reordered from {} to {}", sourceIndex, targetIndex);
                    event.setDropCompleted(true);
                } else {
                    event.setDropCompleted(false);
                }
            }
            
            cell.setStyle("");
            event.consume();
        });
        
        // Handle drag done
        cell.setOnDragDone(event -> {
            cell.setStyle("");
            event.consume();
        });
    }
    
    /**
     * Result of a drag-and-drop reorder operation.
     */
    public static class DragResult<T> {
        private final T item;
        private final int fromIndex;
        private final int toIndex;
        
        public DragResult(T item, int fromIndex, int toIndex) {
            this.item = item;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }
        
        public T getItem() {
            return item;
        }
        
        public int getFromIndex() {
            return fromIndex;
        }
        
        public int getToIndex() {
            return toIndex;
        }
        
        public boolean isReordered() {
            return fromIndex != toIndex;
        }
        
        @Override
        public String toString() {
            return String.format("DragResult{item=%s, from=%d, to=%d}", 
                item, fromIndex, toIndex);
        }
    }
}
