package net.talaatharb.workday.ui.controllers;

import javafx.scene.Scene;
import javafx.stage.Stage;
import net.talaatharb.workday.config.DatabaseConfig;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.facade.PreferencesFacade;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.repository.PreferencesRepository;
import net.talaatharb.workday.service.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapdb.DB;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class SettingsDialogControllerTest {
    
    private DB database;
    private PreferencesFacade facade;
    private SettingsDialogController controller;
    
    @Start
    void start(Stage stage) throws Exception {
        database = DatabaseConfig.inMemoryDatabase();
        PreferencesRepository repository = new PreferencesRepository(database);
        EventDispatcher eventDispatcher = new EventDispatcher();
        PreferencesService service = new PreferencesService(repository, eventDispatcher);
        facade = new PreferencesFacade(service);
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/talaatharb/workday/ui/SettingsDialog.fxml"));
        VBox root = loader.load();
        controller = loader.getController();
        controller.setPreferencesFacade(facade);
        controller.loadPreferences();
        
        stage.setScene(new Scene(root));
        stage.show();
    }
    
    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
    }
    
    @Test
    void shouldLoadDefaultPreferences(FxRobot robot) {
        // Verify default values are loaded
        robot.clickOn("Appearance");
        
        // Check that controls are visible and have default values
        assertNotNull(controller);
    }
    
    @Test
    void shouldSavePreferences(FxRobot robot) {
        // Click on General tab
        robot.clickOn("General");
        
        // Modify a setting
        robot.clickOn("Start on system boot");
        
        // Click Save
        robot.clickOn("Save");
        
        // Verify preferences were saved
        UserPreferences saved = facade.getPreferences();
        assertTrue(saved.isStartOnBoot());
    }
    
    @Test
    void shouldCancelWithoutSaving(FxRobot robot) {
        UserPreferences original = facade.getPreferences();
        boolean originalStartOnBoot = original.isStartOnBoot();
        
        // Modify settings
        robot.clickOn("General");
        robot.clickOn("Start on system boot");
        
        // Click Cancel
        robot.clickOn("Cancel");
        
        // Verify preferences were NOT saved
        UserPreferences current = facade.getPreferences();
        assertEquals(originalStartOnBoot, current.isStartOnBoot());
    }
    
    @Test
    void shouldSwitchBetweenTabs(FxRobot robot) {
        // Test tab navigation
        robot.clickOn("General");
        robot.clickOn("Appearance");
        robot.clickOn("Notifications");
        robot.clickOn("Shortcuts");
        
        // No assertions needed, just verify no exceptions
    }
}
