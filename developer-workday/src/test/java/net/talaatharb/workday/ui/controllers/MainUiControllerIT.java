package net.talaatharb.workday.ui.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.application.Platform;

@ExtendWith(MockitoExtension.class)
class MainUiControllerIT extends ApplicationTest {

    @InjectMocks
    MainUiController uiController;

    @BeforeEach
    void initializeController() {
        Platform.runLater(() -> {
            uiController.initialize(null, null);
        });
    }

}
