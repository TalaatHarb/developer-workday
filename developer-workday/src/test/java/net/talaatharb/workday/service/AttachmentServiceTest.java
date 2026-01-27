package net.talaatharb.workday.service;

import net.talaatharb.workday.model.Attachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AttachmentService.
 */
class AttachmentServiceTest {
    
    @TempDir
    Path tempDir;
    
    private AttachmentService attachmentService;
    
    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(tempDir.toString());
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup is automatic with @TempDir
    }
    
    @Test
    void testAddAttachment_Success() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        
        // When
        Attachment attachment = attachmentService.addAttachment(testFile, "test.txt");
        
        // Then
        assertNotNull(attachment);
        assertEquals("test.txt", attachment.getFileName());
        assertTrue(attachment.getFilePath().contains("test.txt"));
        assertTrue(attachment.getFileSizeBytes() > 0);
        assertNotNull(attachment.getUploadedAt());
        
        // Verify file was copied
        Path attachmentPath = attachmentService.getAttachmentPath(attachment);
        assertTrue(Files.exists(attachmentPath));
    }
    
    @Test
    void testAddAttachment_FileTooLarge() throws IOException {
        // Given: Create a file larger than the limit (10 MB)
        Path largeFile = tempDir.resolve("large.txt");
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        Files.write(largeFile, largeContent);
        
        // When/Then
        assertThrows(IllegalArgumentException.class, 
            () -> attachmentService.addAttachment(largeFile, "large.txt"));
    }
    
    @Test
    void testDeleteAttachment_Success() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        Attachment attachment = attachmentService.addAttachment(testFile, "test.txt");
        
        // Verify file exists
        Path attachmentPath = attachmentService.getAttachmentPath(attachment);
        assertTrue(Files.exists(attachmentPath));
        
        // When
        boolean deleted = attachmentService.deleteAttachment(attachment);
        
        // Then
        assertTrue(deleted);
        assertFalse(Files.exists(attachmentPath));
    }
    
    @Test
    void testIsFileSizeWithinLimit() {
        // Given
        long smallSize = 1024; // 1 KB
        long largeSize = 15 * 1024 * 1024; // 15 MB
        
        // Then
        assertTrue(attachmentService.isFileSizeWithinLimit(smallSize));
        assertFalse(attachmentService.isFileSizeWithinLimit(largeSize));
    }
    
    @Test
    void testGetMaxFileSizeBytes() {
        // Then
        assertEquals(10 * 1024 * 1024, attachmentService.getMaxFileSizeBytes());
    }
    
    @Test
    void testFormatFileSize() {
        // Then
        assertEquals("500 B", attachmentService.formatFileSize(500));
        assertEquals("1.00 KB", attachmentService.formatFileSize(1024));
        assertEquals("1.00 MB", attachmentService.formatFileSize(1024 * 1024));
    }
    
    @Test
    void testGetMaxFileSizeFormatted() {
        // Then
        assertEquals("10.00 MB", attachmentService.getMaxFileSizeFormatted());
    }
}
