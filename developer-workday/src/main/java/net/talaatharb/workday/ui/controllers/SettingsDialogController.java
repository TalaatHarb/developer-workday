package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.facade.PreferencesFacade;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.utils.ThemeManager;

/**
 * Controller for the settings/preferences dialog.
 */
@Slf4j
public class SettingsDialogController implements Initializable {
    
    // General Tab
    @FXML private CheckBox startOnBootCheckbox;
    @FXML private CheckBox minimizeToTrayCheckbox;
    @FXML private ChoiceBox<String> defaultViewChoice;
    @FXML private ChoiceBox<String> languageChoice;
    
    // Appearance Tab
    @FXML private ChoiceBox<String> themeChoice;
    @FXML private ColorPicker accentColorPicker;
    @FXML private Spinner<Integer> fontSizeSpinner;
    
    // Notifications Tab
    @FXML private CheckBox notificationsEnabledCheckbox;
    @FXML private CheckBox soundEnabledCheckbox;
    @FXML private Spinner<Integer> reminderLeadTimeSpinner;
    
    // Shortcuts Tab (read-only)
    @FXML private Label quickAddShortcutLabel;
    @FXML private Label settingsShortcutLabel;
    
    // Footer Buttons
    @FXML private Button resetButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    
    @Setter private PreferencesFacade preferencesFacade;
    private UserPreferences currentPreferences;
    private Runnable onCloseCallback;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing SettingsDialogController");
        
        // Setup choice boxes
        setupChoiceBoxes();
        
        // Setup spinners
        setupSpinners();
        
        log.info("SettingsDialogController initialized");
    }
    
    /**
     * Load preferences from facade and populate UI.
     */
    public void loadPreferences() {
        if (preferencesFacade != null) {
            try {
                currentPreferences = preferencesFacade.getPreferences();
                populateUIFromPreferences(currentPreferences);
                log.debug("Loaded user preferences");
            } catch (Exception e) {
                log.error("Failed to load preferences", e);
            }
        }
    }
    
    private void setupChoiceBoxes() {
        // Default view choices
        defaultViewChoice.setItems(FXCollections.observableArrayList(
            "Today", "Upcoming", "Calendar", "All Tasks"
        ));
        defaultViewChoice.setValue("Today");
        
        // Language choices
        languageChoice.setItems(FXCollections.observableArrayList(
            "English", "Español", "Français", "Deutsch"
        ));
        languageChoice.setValue("English");
        
        // Theme choices
        themeChoice.setItems(FXCollections.observableArrayList(
            "Light", "Dark"
        ));
        themeChoice.setValue("Light");
    }
    
    private void setupSpinners() {
        // Font size spinner (8-24 pt)
        SpinnerValueFactory<Integer> fontSizeFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 24, 13, 1);
        fontSizeSpinner.setValueFactory(fontSizeFactory);
        
        // Reminder lead time spinner (0-60 minutes)
        SpinnerValueFactory<Integer> reminderFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 15, 5);
        reminderLeadTimeSpinner.setValueFactory(reminderFactory);
    }
    
    private void populateUIFromPreferences(UserPreferences prefs) {
        // General settings
        startOnBootCheckbox.setSelected(prefs.isStartOnBoot());
        minimizeToTrayCheckbox.setSelected(prefs.isMinimizeToTray());
        defaultViewChoice.setValue(mapDefaultViewToChoice(prefs.getDefaultView()));
        languageChoice.setValue(mapLanguageToChoice(prefs.getLanguage()));
        
        // Appearance settings
        themeChoice.setValue(mapThemeToChoice(prefs.getTheme()));
        accentColorPicker.setValue(Color.web(prefs.getAccentColor()));
        fontSizeSpinner.getValueFactory().setValue(prefs.getFontSize());
        
        // Notification settings
        notificationsEnabledCheckbox.setSelected(prefs.isDesktopNotificationsEnabled());
        soundEnabledCheckbox.setSelected(prefs.isSoundEnabled());
        reminderLeadTimeSpinner.getValueFactory().setValue(prefs.getReminderLeadTimeMinutes());
        
        // Shortcuts (read-only)
        quickAddShortcutLabel.setText(prefs.getQuickAddShortcut());
        settingsShortcutLabel.setText(prefs.getSettingsShortcut());
    }
    
    private UserPreferences buildPreferencesFromUI() {
        return UserPreferences.builder()
            .id(currentPreferences != null ? currentPreferences.getId() : null)
            .createdAt(currentPreferences != null ? currentPreferences.getCreatedAt() : null)
            // General
            .startOnBoot(startOnBootCheckbox.isSelected())
            .minimizeToTray(minimizeToTrayCheckbox.isSelected())
            .defaultView(mapChoiceToDefaultView(defaultViewChoice.getValue()))
            .language(mapChoiceToLanguage(languageChoice.getValue()))
            // Appearance
            .theme(mapChoiceToTheme(themeChoice.getValue()))
            .accentColor(toHexColor(accentColorPicker.getValue()))
            .fontSize(fontSizeSpinner.getValue())
            // Notifications
            .desktopNotificationsEnabled(notificationsEnabledCheckbox.isSelected())
            .soundEnabled(soundEnabledCheckbox.isSelected())
            .reminderLeadTimeMinutes(reminderLeadTimeSpinner.getValue())
            // Shortcuts (unchanged)
            .quickAddShortcut(quickAddShortcutLabel.getText())
            .settingsShortcut(settingsShortcutLabel.getText())
            .build();
    }
    
    @FXML
    private void handleSave() {
        if (preferencesFacade != null) {
            try {
                UserPreferences preferences = buildPreferencesFromUI();
                UserPreferences saved = preferencesFacade.updatePreferences(preferences);
                
                // Apply theme immediately if changed
                if (!saved.getTheme().equals(currentPreferences.getTheme())) {
                    ThemeManager.getInstance().applyTheme(saved.getTheme());
                }
                
                log.info("Saved user preferences");
                closeDialog();
            } catch (Exception e) {
                log.error("Failed to save preferences", e);
                showError("Failed to save preferences: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleCancel() {
        log.debug("Settings dialog cancelled");
        closeDialog();
    }
    
    @FXML
    private void handleReset() {
        if (preferencesFacade != null) {
            // Show confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Reset Settings");
            confirm.setHeaderText("Reset all settings to defaults?");
            confirm.setContentText("This action cannot be undone.");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        UserPreferences defaults = preferencesFacade.resetToDefaults();
                        populateUIFromPreferences(defaults);
                        currentPreferences = defaults;
                        log.info("Reset preferences to defaults");
                    } catch (Exception e) {
                        log.error("Failed to reset preferences", e);
                        showError("Failed to reset preferences: " + e.getMessage());
                    }
                }
            });
        }
    }
    
    private void closeDialog() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Settings Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Mapping helpers
    private String mapDefaultViewToChoice(String defaultView) {
        switch (defaultView.toLowerCase()) {
            case "today": return "Today";
            case "upcoming": return "Upcoming";
            case "calendar": return "Calendar";
            case "all": return "All Tasks";
            default: return "Today";
        }
    }
    
    private String mapChoiceToDefaultView(String choice) {
        switch (choice) {
            case "Today": return "today";
            case "Upcoming": return "upcoming";
            case "Calendar": return "calendar";
            case "All Tasks": return "all";
            default: return "today";
        }
    }
    
    private String mapLanguageToChoice(String language) {
        switch (language.toLowerCase()) {
            case "en": return "English";
            case "es": return "Español";
            case "fr": return "Français";
            case "de": return "Deutsch";
            default: return "English";
        }
    }
    
    private String mapChoiceToLanguage(String choice) {
        switch (choice) {
            case "English": return "en";
            case "Español": return "es";
            case "Français": return "fr";
            case "Deutsch": return "de";
            default: return "en";
        }
    }
    
    private String mapThemeToChoice(String theme) {
        return "dark".equalsIgnoreCase(theme) ? "Dark" : "Light";
    }
    
    private String mapChoiceToTheme(String choice) {
        return "Dark".equals(choice) ? "dark" : "light";
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
}
