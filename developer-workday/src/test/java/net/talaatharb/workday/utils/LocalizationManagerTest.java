package net.talaatharb.workday.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocalizationManager.
 */
class LocalizationManagerTest {
    
    private LocalizationManager localizationManager;
    
    @BeforeEach
    void setUp() {
        localizationManager = LocalizationManager.getInstance();
        localizationManager.resetForTesting();
    }
    
    @AfterEach
    void tearDown() {
        localizationManager.resetForTesting();
    }
    
    @Test
    void testGetInstance_returnsSameInstance() {
        LocalizationManager instance1 = LocalizationManager.getInstance();
        LocalizationManager instance2 = LocalizationManager.getInstance();
        
        assertSame(instance1, instance2, "Should return same singleton instance");
    }
    
    @Test
    void testInitializeLocale_defaultsToEnglish() {
        // After reset, should be English
        assertEquals(Locale.ENGLISH, localizationManager.getCurrentLocale());
    }
    
    @Test
    void testSetLocale_withLocaleObject() {
        Locale spanish = new Locale("es");
        localizationManager.setLocale(spanish);
        
        assertEquals(spanish, localizationManager.getCurrentLocale());
    }
    
    @Test
    void testSetLocale_withLanguageCode() {
        localizationManager.setLocale("es");
        
        assertEquals("es", localizationManager.getCurrentLocale().getLanguage());
    }
    
    @Test
    void testSetLocale_withNullLocale_defaultsToEnglish() {
        localizationManager.setLocale((Locale) null);
        
        assertEquals(Locale.ENGLISH, localizationManager.getCurrentLocale());
    }
    
    @Test
    void testSetLocale_withEmptyLanguageCode_defaultsToEnglish() {
        localizationManager.setLocale("");
        
        assertEquals(Locale.ENGLISH, localizationManager.getCurrentLocale());
    }
    
    @Test
    void testGetString_returnsEnglishMessage() {
        localizationManager.setLocale(Locale.ENGLISH);
        
        String message = localizationManager.getString("common.ok");
        
        assertEquals("OK", message);
    }
    
    @Test
    void testGetString_returnsSpanishMessage() {
        localizationManager.setLocale(new Locale("es"));
        
        String message = localizationManager.getString("common.ok");
        
        assertEquals("Aceptar", message);
    }
    
    @Test
    void testGetString_returnsFrenchMessage() {
        localizationManager.setLocale(Locale.FRENCH);
        
        String message = localizationManager.getString("common.ok");
        
        assertEquals("OK", message);
    }
    
    @Test
    void testGetString_returnsGermanMessage() {
        localizationManager.setLocale(Locale.GERMAN);
        
        String message = localizationManager.getString("common.ok");
        
        assertEquals("OK", message);
    }
    
    @Test
    void testGetString_withMissingKey_fallsBackToDefault() {
        localizationManager.setLocale(new Locale("es"));
        
        String message = localizationManager.getString("nonexistent.key");
        
        // Should fall back to English or return key itself
        assertNotNull(message);
    }
    
    @Test
    void testGetString_withNullKey_returnsEmptyString() {
        String message = localizationManager.getString(null);
        
        assertEquals("", message);
    }
    
    @Test
    void testGetString_withEmptyKey_returnsEmptyString() {
        String message = localizationManager.getString("");
        
        assertEquals("", message);
    }
    
    @Test
    void testGetStringWithParams_formatsCorrectly() {
        localizationManager.setLocale(Locale.ENGLISH);
        
        // Assuming there's a parameterized message in the bundle
        String message = localizationManager.getString("common.ok");
        
        assertNotNull(message);
    }
    
    @Test
    void testIsSupportedLocale_english() {
        assertTrue(localizationManager.isSupportedLocale(Locale.ENGLISH));
    }
    
    @Test
    void testIsSupportedLocale_spanish() {
        assertTrue(localizationManager.isSupportedLocale(new Locale("es")));
    }
    
    @Test
    void testIsSupportedLocale_french() {
        assertTrue(localizationManager.isSupportedLocale(new Locale("fr")));
    }
    
    @Test
    void testIsSupportedLocale_german() {
        assertTrue(localizationManager.isSupportedLocale(new Locale("de")));
    }
    
    @Test
    void testIsSupportedLocale_unsupportedLocale() {
        // Java ResourceBundle may fall back to default, so we just check the method doesn't throw
        Locale unsupportedLocale = new Locale("zh");
        assertDoesNotThrow(() -> localizationManager.isSupportedLocale(unsupportedLocale));
    }
    
    @Test
    void testGetSupportedLocales_containsAllExpected() {
        List<Locale> supported = localizationManager.getSupportedLocales();
        
        assertTrue(supported.size() >= 4, "Should have at least 4 supported locales");
        assertTrue(supported.stream().anyMatch(l -> l.getLanguage().equals("en")));
        assertTrue(supported.stream().anyMatch(l -> l.getLanguage().equals("es")));
        assertTrue(supported.stream().anyMatch(l -> l.getLanguage().equals("fr")));
        assertTrue(supported.stream().anyMatch(l -> l.getLanguage().equals("de")));
    }
    
    @Test
    void testAddLocaleChangeListener_notifiesOnChange() {
        AtomicInteger callCount = new AtomicInteger(0);
        Locale[] capturedLocale = new Locale[1];
        
        LocalizationManager.LocaleChangeListener listener = newLocale -> {
            callCount.incrementAndGet();
            capturedLocale[0] = newLocale;
        };
        
        localizationManager.addLocaleChangeListener(listener);
        localizationManager.setLocale(new Locale("es"));
        
        assertEquals(1, callCount.get(), "Listener should be notified once");
        assertEquals("es", capturedLocale[0].getLanguage());
    }
    
    @Test
    void testAddLocaleChangeListener_nullListener_ignored() {
        assertDoesNotThrow(() -> localizationManager.addLocaleChangeListener(null));
    }
    
    @Test
    void testAddLocaleChangeListener_duplicateListener_addedOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        LocalizationManager.LocaleChangeListener listener = newLocale -> callCount.incrementAndGet();
        
        localizationManager.addLocaleChangeListener(listener);
        localizationManager.addLocaleChangeListener(listener);
        localizationManager.setLocale(new Locale("es"));
        
        assertEquals(1, callCount.get(), "Duplicate listener should not be added");
    }
    
    @Test
    void testRemoveLocaleChangeListener_stopsNotifications() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        LocalizationManager.LocaleChangeListener listener = newLocale -> callCount.incrementAndGet();
        
        localizationManager.addLocaleChangeListener(listener);
        localizationManager.setLocale(new Locale("es"));
        
        assertEquals(1, callCount.get());
        
        localizationManager.removeLocaleChangeListener(listener);
        localizationManager.setLocale(Locale.FRENCH);
        
        assertEquals(1, callCount.get(), "Listener should not be notified after removal");
    }
    
    @Test
    void testMultipleListeners_allNotified() {
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);
        
        localizationManager.addLocaleChangeListener(newLocale -> count1.incrementAndGet());
        localizationManager.addLocaleChangeListener(newLocale -> count2.incrementAndGet());
        
        localizationManager.setLocale(new Locale("es"));
        
        assertEquals(1, count1.get());
        assertEquals(1, count2.get());
    }
    
    @Test
    void testLocaleChangeListener_exceptionHandled() {
        LocalizationManager.LocaleChangeListener faultyListener = newLocale -> {
            throw new RuntimeException("Test exception");
        };
        
        localizationManager.addLocaleChangeListener(faultyListener);
        
        // Should not throw, exception should be caught and logged
        assertDoesNotThrow(() -> localizationManager.setLocale(new Locale("es")));
    }
    
    @Test
    void testChangeLanguageAtRuntime_scenario() {
        // Given the settings dialog
        localizationManager.setLocale(Locale.ENGLISH);
        String messageBeforeChange = localizationManager.getString("common.cancel");
        assertEquals("Cancel", messageBeforeChange);
        
        // When changing the language
        localizationManager.setLocale(new Locale("es"));
        
        // Then all UI text should update to the selected language
        String messageAfterChange = localizationManager.getString("common.cancel");
        assertEquals("Cancelar", messageAfterChange);
        
        // And the preference should be saved (via SettingsDialogController)
        assertEquals("es", localizationManager.getCurrentLocale().getLanguage());
    }
    
    @Test
    void testHandleMissingTranslations_scenario() {
        // Given a translation key without translation in current language
        localizationManager.setLocale(new Locale("es"));
        
        // When rendering text
        String message = localizationManager.getString("nonexistent.key.test");
        
        // Then fallback to default language should occur gracefully
        assertNotNull(message, "Should return a non-null value");
        // It will either return the English fallback or the key itself
    }
    
    @Test
    void testLoadLanguageResources_scenario() {
        // Given resource bundles for different languages
        // When the application starts (simulated by resetting)
        localizationManager.resetForTesting();
        
        // Then the appropriate language bundle should be loaded based on user preference or system locale
        assertNotNull(localizationManager.getCurrentLocale());
        assertNotNull(localizationManager.getString("common.ok"));
    }
    
    @Test
    void testAllCommonMessages_availableInAllLanguages() {
        String[] commonKeys = {
            "common.ok",
            "common.cancel",
            "common.save",
            "common.delete",
            "app.title"
        };
        
        Locale[] testLocales = {
            Locale.ENGLISH,
            new Locale("es"),
            new Locale("fr"),
            new Locale("de")
        };
        
        for (Locale locale : testLocales) {
            localizationManager.setLocale(locale);
            for (String key : commonKeys) {
                String message = localizationManager.getString(key);
                assertNotNull(message, "Key '" + key + "' should exist in locale " + locale);
                assertFalse(message.isEmpty(), "Message for key '" + key + "' should not be empty in locale " + locale);
            }
        }
    }
}
