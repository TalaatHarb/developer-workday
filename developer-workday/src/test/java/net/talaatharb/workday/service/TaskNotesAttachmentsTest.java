package net.talaatharb.workday.service;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.model.Attachment;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Task notes and attachments functionality.
 */
class TaskNotesAttachmentsTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private TaskService taskService;
    
    @BeforeEach
    void setUp() {
        database = DBMaker.memoryDB().make();
        taskRepository = new TaskRepository(database);
        eventDispatcher = new EventDispatcher();
        taskService = new TaskService(taskRepository, eventDispatcher);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
    }
    
    @Test
    void testUpdateNotes_Success() {
        // Given
        Task task = Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .build();
        Task savedTask = taskService.createTask(task);
        
        String notes = "<html><body><h1>Test Notes</h1><p>This is a test note with <b>rich text</b>.</p></body></html>";
        
        // When
        Task updatedTask = taskService.updateNotes(savedTask.getId(), notes);
        
        // Then
        assertNotNull(updatedTask);
        assertEquals(notes, updatedTask.getNotes());
        assertNotNull(updatedTask.getUpdatedAt());
    }
    
    @Test
    void testUpdateNotes_TaskNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        
        // When/Then
        assertThrows(IllegalArgumentException.class, 
            () -> taskService.updateNotes(nonExistentId, "Notes"));
    }
    
    @Test
    void testAddAttachment_Success() {
        // Given
        Task task = Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .attachments(new ArrayList<>())
            .build();
        Task savedTask = taskService.createTask(task);
        
        Attachment attachment = Attachment.builder()
            .fileName("test.pdf")
            .filePath("path/to/test.pdf")
            .fileSizeBytes(1024)
            .mimeType("application/pdf")
            .uploadedAt(LocalDateTime.now())
            .build();
        
        // When
        Task updatedTask = taskService.addAttachment(savedTask.getId(), attachment);
        
        // Then
        assertNotNull(updatedTask);
        assertNotNull(updatedTask.getAttachments());
        assertEquals(1, updatedTask.getAttachments().size());
        assertEquals("test.pdf", updatedTask.getAttachments().get(0).getFileName());
    }
    
    @Test
    void testAddAttachment_InitializesListIfNull() {
        // Given
        Task task = Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .build();
        Task savedTask = taskService.createTask(task);
        
        Attachment attachment = Attachment.builder()
            .fileName("test.pdf")
            .filePath("path/to/test.pdf")
            .fileSizeBytes(1024)
            .mimeType("application/pdf")
            .uploadedAt(LocalDateTime.now())
            .build();
        
        // When
        Task updatedTask = taskService.addAttachment(savedTask.getId(), attachment);
        
        // Then
        assertNotNull(updatedTask.getAttachments());
        assertEquals(1, updatedTask.getAttachments().size());
    }
    
    @Test
    void testRemoveAttachment_Success() {
        // Given
        Attachment attachment1 = Attachment.builder()
            .fileName("test1.pdf")
            .filePath("path/to/test1.pdf")
            .fileSizeBytes(1024)
            .mimeType("application/pdf")
            .uploadedAt(LocalDateTime.now())
            .build();
        
        Attachment attachment2 = Attachment.builder()
            .fileName("test2.pdf")
            .filePath("path/to/test2.pdf")
            .fileSizeBytes(2048)
            .mimeType("application/pdf")
            .uploadedAt(LocalDateTime.now())
            .build();
        
        ArrayList<Attachment> attachments = new ArrayList<>();
        attachments.add(attachment1);
        attachments.add(attachment2);
        
        Task task = Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .attachments(attachments)
            .build();
        Task savedTask = taskService.createTask(task);
        
        // When
        Task updatedTask = taskService.removeAttachment(savedTask.getId(), "test1.pdf");
        
        // Then
        assertNotNull(updatedTask);
        assertEquals(1, updatedTask.getAttachments().size());
        assertEquals("test2.pdf", updatedTask.getAttachments().get(0).getFileName());
    }
    
    @Test
    void testRemoveAttachment_NoAttachments() {
        // Given
        Task task = Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .build();
        Task savedTask = taskService.createTask(task);
        
        // When
        Task result = taskService.removeAttachment(savedTask.getId(), "nonexistent.pdf");
        
        // Then
        assertNotNull(result);
        // No exception should be thrown
    }
}
