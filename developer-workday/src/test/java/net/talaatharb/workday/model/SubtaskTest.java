package net.talaatharb.workday.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Subtask model.
 */
class SubtaskTest {

    @Test
    void testSubtaskCreation() {
        // Given
        UUID id = UUID.randomUUID();
        String title = "Test Subtask";
        LocalDateTime now = LocalDateTime.now();

        // When
        Subtask subtask = Subtask.builder()
                .id(id)
                .title(title)
                .completed(false)
                .sortOrder(0)
                .createdAt(now)
                .build();

        // Then
        assertNotNull(subtask);
        assertEquals(id, subtask.getId());
        assertEquals(title, subtask.getTitle());
        assertFalse(subtask.isCompleted());
        assertEquals(0, subtask.getSortOrder());
        assertEquals(now, subtask.getCreatedAt());
        assertNull(subtask.getCompletedAt());
    }

    @Test
    void testSubtaskCompletion() {
        // Given
        Subtask subtask = Subtask.builder()
                .id(UUID.randomUUID())
                .title("Test Subtask")
                .completed(false)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        subtask.setCompleted(true);
        LocalDateTime completedAt = LocalDateTime.now();
        subtask.setCompletedAt(completedAt);

        // Then
        assertTrue(subtask.isCompleted());
        assertEquals(completedAt, subtask.getCompletedAt());
    }

    @Test
    void testSubtaskSortOrder() {
        // Given
        Subtask subtask1 = Subtask.builder()
                .id(UUID.randomUUID())
                .title("Subtask 1")
                .sortOrder(0)
                .build();

        Subtask subtask2 = Subtask.builder()
                .id(UUID.randomUUID())
                .title("Subtask 2")
                .sortOrder(1)
                .build();

        // Then
        assertTrue(subtask1.getSortOrder() < subtask2.getSortOrder());
    }

    @Test
    void testSubtaskSerializable() {
        // Given
        Subtask subtask = Subtask.builder()
                .id(UUID.randomUUID())
                .title("Test Subtask")
                .completed(false)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build();

        // Then - should implement Serializable
        assertTrue(subtask instanceof java.io.Serializable);
    }
}
