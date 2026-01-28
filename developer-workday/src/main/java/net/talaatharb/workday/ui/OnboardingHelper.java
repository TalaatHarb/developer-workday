package net.talaatharb.workday.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper class for first-time user onboarding.
 * Shows a welcome dialog with key features and setup options.
 */
@Slf4j
public class OnboardingHelper {
    
    private static final String ONBOARDING_FILE = ".onboarding-completed";
    
    /**
     * Check if onboarding should be shown
     */
    public boolean shouldShowOnboarding() {
        Path onboardingFile = getOnboardingFilePath();
        return !Files.exists(onboardingFile);
    }
    
    /**
     * Show onboarding dialog
     */
    public void showOnboarding(Stage owner, Runnable onComplete) {
        log.info("Showing onboarding dialog");
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Welcome to Developer Workday");
        dialog.setResizable(false);
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: white;");
        
        // Welcome header
        Label welcomeLabel = new Label("👋 Welcome!");
        welcomeLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        
        // Description
        Label descLabel = new Label(
            "Developer Workday helps you manage tasks efficiently.\n" +
            "Here's what you can do:"
        );
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-alignment: center;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(400);
        
        // Features list
        VBox features = new VBox(10);
        features.setAlignment(Pos.CENTER_LEFT);
        features.setMaxWidth(400);
        
        features.getChildren().addAll(
            createFeatureItem("✅", "Create and organize tasks"),
            createFeatureItem("📁", "Group tasks into categories"),
            createFeatureItem("📅", "Schedule tasks with due dates"),
            createFeatureItem("⏰", "Set reminders and priorities"),
            createFeatureItem("🎯", "Track completed tasks"),
            createFeatureItem("⌨️", "Use keyboard shortcuts for speed")
        );
        
        // Sample data option
        CheckBox createSamplesCheck = new CheckBox("Create sample tasks and categories");
        createSamplesCheck.setSelected(true);
        createSamplesCheck.setStyle("-fx-font-size: 12px;");
        
        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button skipButton = new Button("Skip");
        skipButton.setStyle("-fx-padding: 8 20 8 20;");
        skipButton.setOnAction(e -> {
            markOnboardingSkipped();
            dialog.close();
            log.info("Onboarding skipped");
        });
        
        Button getStartedButton = new Button("Get Started");
        getStartedButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                                 "-fx-padding: 8 20 8 20; -fx-font-weight: bold;");
        getStartedButton.setOnAction(e -> {
            boolean createSamples = createSamplesCheck.isSelected();
            markOnboardingCompleted();
            dialog.close();
            
            if (onComplete != null) {
                onComplete.run();
            }
            
            if (createSamples && onComplete != null) {
                // Signal to create sample data
                log.info("Creating sample data");
            }
            
            log.info("Onboarding completed");
        });
        
        buttonBox.getChildren().addAll(skipButton, getStartedButton);
        
        // Add all to root
        root.getChildren().addAll(
            welcomeLabel,
            descLabel,
            features,
            createSamplesCheck,
            buttonBox
        );
        
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /**
     * Create a feature list item
     */
    private HBox createFeatureItem(String icon, String text) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px;");
        
        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 12px;");
        
        item.getChildren().addAll(iconLabel, textLabel);
        return item;
    }
    
    /**
     * Mark onboarding as completed
     */
    private void markOnboardingCompleted() {
        try {
            Path onboardingFile = getOnboardingFilePath();
            Files.createDirectories(onboardingFile.getParent());
            Files.createFile(onboardingFile);
        } catch (IOException e) {
            log.error("Failed to mark onboarding as completed", e);
        }
    }
    
    /**
     * Mark onboarding as skipped
     */
    private void markOnboardingSkipped() {
        markOnboardingCompleted(); // Same action
    }
    
    /**
     * Reset onboarding state (for testing or re-showing)
     */
    public void resetOnboarding() {
        try {
            Path onboardingFile = getOnboardingFilePath();
            Files.deleteIfExists(onboardingFile);
            log.info("Onboarding state reset");
        } catch (IOException e) {
            log.error("Failed to reset onboarding state", e);
        }
    }
    
    /**
     * Get the path to the onboarding marker file
     */
    private Path getOnboardingFilePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".developer-workday", ONBOARDING_FILE);
    }
}
