package net.talaatharb.workday.service;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Attachment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing task attachments.
 */
@Slf4j
public class AttachmentService {
    
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final String ATTACHMENTS_DIR = "attachments";
    private final Path attachmentsBasePath;
    
    public AttachmentService(String appDataDirectory) {
        this.attachmentsBasePath = Paths.get(appDataDirectory, ATTACHMENTS_DIR);
        try {
            Files.createDirectories(attachmentsBasePath);
            log.info("Attachments directory initialized at: {}", attachmentsBasePath);
        } catch (IOException e) {
            log.error("Failed to create attachments directory", e);
        }
    }
    
    /**
     * Add an attachment by copying a file.
     * @param sourcePath The source file path
     * @param originalFileName The original file name
     * @return Attachment object with metadata
     * @throws IOException if file operations fail
     * @throws IllegalArgumentException if file size exceeds limit
     */
    public Attachment addAttachment(Path sourcePath, String originalFileName) throws IOException {
        // Check file size
        long fileSize = Files.size(sourcePath);
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                    fileSize, MAX_FILE_SIZE_BYTES));
        }
        
        // Generate unique file name to avoid conflicts
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        Path targetPath = attachmentsBasePath.resolve(uniqueFileName);
        
        // Copy file
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Attachment added: {} -> {}", originalFileName, uniqueFileName);
        
        // Create attachment metadata
        return Attachment.builder()
            .fileName(originalFileName)
            .filePath(uniqueFileName)
            .fileSizeBytes(fileSize)
            .mimeType(detectMimeType(sourcePath))
            .uploadedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Delete an attachment file.
     * @param attachment The attachment to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteAttachment(Attachment attachment) {
        try {
            Path filePath = attachmentsBasePath.resolve(attachment.getFilePath());
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Attachment deleted: {}", attachment.getFileName());
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete attachment: {}", attachment.getFileName(), e);
            return false;
        }
    }
    
    /**
     * Get the full path to an attachment file.
     * @param attachment The attachment
     * @return Full path to the file
     */
    public Path getAttachmentPath(Attachment attachment) {
        return attachmentsBasePath.resolve(attachment.getFilePath());
    }
    
    /**
     * Check if a file size is within limits.
     * @param fileSizeBytes The file size in bytes
     * @return true if within limits
     */
    public boolean isFileSizeWithinLimit(long fileSizeBytes) {
        return fileSizeBytes <= MAX_FILE_SIZE_BYTES;
    }
    
    /**
     * Get the maximum file size limit.
     * @return Maximum file size in bytes
     */
    public long getMaxFileSizeBytes() {
        return MAX_FILE_SIZE_BYTES;
    }
    
    /**
     * Get the maximum file size in a human-readable format.
     * @return Formatted file size string
     */
    public String getMaxFileSizeFormatted() {
        return formatFileSize(MAX_FILE_SIZE_BYTES);
    }
    
    /**
     * Format file size in human-readable format.
     * @param bytes File size in bytes
     * @return Formatted string (e.g., "10 MB")
     */
    public String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Detect MIME type of a file.
     * @param path File path
     * @return MIME type string
     */
    private String detectMimeType(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null ? mimeType : "application/octet-stream";
        } catch (IOException e) {
            log.warn("Failed to detect MIME type for: {}", path, e);
            return "application/octet-stream";
        }
    }
}
