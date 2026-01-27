package net.talaatharb.workday.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages localization and internationalization for the application.
 * Provides methods to load resource bundles, change language at runtime,
 * and handle missing translations with fallback support.
 */
@Slf4j
public class LocalizationManager {
    
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    
    private static LocalizationManager instance;
    
    @Getter
    private Locale currentLocale;
    private ResourceBundle currentBundle;
    private final List<LocaleChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    private LocalizationManager() {
        // Initialize with system locale or default
        initializeLocale();
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized LocalizationManager getInstance() {
        if (instance == null) {
            instance = new LocalizationManager();
        }
        return instance;
    }
    
    /**
     * Initialize locale from system or default.
     */
    private void initializeLocale() {
        try {
            Locale systemLocale = Locale.getDefault();
            if (isSupportedLocale(systemLocale)) {
                setLocale(systemLocale);
            } else {
                setLocale(DEFAULT_LOCALE);
            }
            log.info("Initialized localization with locale: {}", currentLocale);
        } catch (Exception e) {
            log.error("Failed to initialize locale, using default", e);
            setLocale(DEFAULT_LOCALE);
        }
    }
    
    /**
     * Set the current locale and reload resource bundle.
     * 
     * @param locale the new locale to set
     */
    public void setLocale(Locale locale) {
        if (locale == null) {
            locale = DEFAULT_LOCALE;
        }
        
        this.currentLocale = locale;
        reloadBundle();
        notifyListeners(locale);
    }
    
    /**
     * Set locale by language code (e.g., "en", "es", "fr", "de").
     * 
     * @param languageCode the ISO language code
     */
    public void setLocale(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            setLocale(DEFAULT_LOCALE);
            return;
        }
        
        Locale locale = Locale.forLanguageTag(languageCode);
        setLocale(locale);
    }
    
    /**
     * Reload the resource bundle for the current locale.
     */
    private void reloadBundle() {
        try {
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
            log.debug("Loaded resource bundle for locale: {}", currentLocale);
        } catch (MissingResourceException e) {
            log.warn("Resource bundle not found for locale {}, falling back to default", currentLocale, e);
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, DEFAULT_LOCALE);
        }
    }
    
    /**
     * Get localized string for the given key.
     * Falls back to default locale if key not found in current locale.
     * 
     * @param key the resource key
     * @return the localized string, or the key itself if not found
     */
    public String getString(String key) {
        if (key == null || key.trim().isEmpty()) {
            return "";
        }
        
        try {
            return currentBundle.getString(key);
        } catch (MissingResourceException e) {
            log.debug("Missing translation for key '{}' in locale {}, trying default", key, currentLocale);
            return getStringFromDefault(key);
        }
    }
    
    /**
     * Get localized string with parameter substitution.
     * 
     * @param key the resource key
     * @param params parameters to substitute in the localized string
     * @return the formatted localized string
     */
    public String getString(String key, Object... params) {
        String template = getString(key);
        try {
            return String.format(template, params);
        } catch (IllegalFormatException e) {
            log.error("Failed to format string for key '{}' with params", key, e);
            return template;
        }
    }
    
    /**
     * Try to get string from default locale bundle.
     */
    private String getStringFromDefault(String key) {
        try {
            ResourceBundle defaultBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, DEFAULT_LOCALE);
            return defaultBundle.getString(key);
        } catch (MissingResourceException e) {
            log.warn("Missing translation for key '{}' even in default locale", key);
            return key; // Return the key itself as last resort
        }
    }
    
    /**
     * Check if a locale is supported by checking if resource bundle exists.
     * 
     * @param locale the locale to check
     * @return true if supported, false otherwise
     */
    public boolean isSupportedLocale(Locale locale) {
        try {
            ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
            return true;
        } catch (MissingResourceException e) {
            return false;
        }
    }
    
    /**
     * Get list of all supported locales.
     * 
     * @return list of supported locales
     */
    public List<Locale> getSupportedLocales() {
        List<Locale> supported = new ArrayList<>();
        
        // Add known supported locales
        Locale[] candidates = {
            Locale.ENGLISH,
            new Locale("es"), // Spanish
            new Locale("fr"), // French
            new Locale("de")  // German
        };
        
        for (Locale locale : candidates) {
            if (isSupportedLocale(locale)) {
                supported.add(locale);
            }
        }
        
        return supported;
    }
    
    /**
     * Add a listener to be notified of locale changes.
     * 
     * @param listener the listener to add
     */
    public void addLocaleChangeListener(LocaleChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a locale change listener.
     * 
     * @param listener the listener to remove
     */
    public void removeLocaleChangeListener(LocaleChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners of locale change.
     */
    private void notifyListeners(Locale newLocale) {
        for (LocaleChangeListener listener : listeners) {
            try {
                listener.onLocaleChanged(newLocale);
            } catch (Exception e) {
                log.error("Error notifying locale change listener", e);
            }
        }
    }
    
    /**
     * Listener interface for locale changes.
     */
    @FunctionalInterface
    public interface LocaleChangeListener {
        /**
         * Called when the locale changes.
         * 
         * @param newLocale the new locale
         */
        void onLocaleChanged(Locale newLocale);
    }
    
    /**
     * Reset to default locale for testing purposes.
     */
    void resetForTesting() {
        setLocale(DEFAULT_LOCALE);
        listeners.clear();
    }
}
