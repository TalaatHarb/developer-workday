package net.talaatharb.workday.ui.controllers;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.application.Platform;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.FlowPane;

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
