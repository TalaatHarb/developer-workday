package net.talaatharb.workday.ui;

import java.awt.*;
import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * System tray integration for Developer Workday application.
 * Provides minimize to tray and quick actions from tray icon.
 */
@Slf4j
public class SystemTrayManager {
    
    private final Stage primaryStage;
    private TrayIcon trayIcon;
    private boolean traySupported;
    
    public SystemTrayManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.traySupported = SystemTray.isSupported();
        
        if (!traySupported) {
            log.warn("System tray is not supported on this platform");
        }
    }
    
    /**
     * Initialize and show system tray icon
     */
    public void initializeTray() {
        if (!traySupported) {
            log.warn("Cannot initialize tray - not supported");
            return;
        }
        
        try {
            // Create tray icon
            SystemTray tray = SystemTray.getSystemTray();
            Image image = createTrayImage();
            
            // Create popup menu
            PopupMenu popup = createPopupMenu();
            
            // Create tray icon
            trayIcon = new TrayIcon(image, "Developer Workday", popup);
            trayIcon.setImageAutoSize(true);
            
            // Double-click to restore window
            trayIcon.addActionListener(e -> restoreWindow());
            
            // Add to system tray
            tray.add(trayIcon);
            
            log.info("System tray icon initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize system tray", e);
        }
    }
    
    /**
     * Create popup menu for tray icon
     */
    private PopupMenu createPopupMenu() {
        PopupMenu popup = new PopupMenu();
        
        // Show window
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(e -> restoreWindow());
        popup.add(showItem);
        
        popup.addSeparator();
        
        // Quick Add
        MenuItem quickAddItem = new MenuItem("Quick Add Task");
        quickAddItem.addActionListener(e -> {
            restoreWindow();
            // TODO: Focus quick add field when implemented
            log.debug("Quick add triggered from tray");
        });
        popup.add(quickAddItem);
        
        // Today's Tasks
        MenuItem todayItem = new MenuItem("Today's Tasks");
        todayItem.addActionListener(e -> {
            restoreWindow();
            // TODO: Navigate to today view when implemented
            log.debug("Today's tasks triggered from tray");
        });
        popup.add(todayItem);
        
        popup.addSeparator();
        
        // Exit
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            log.info("Exit triggered from system tray");
            Platform.exit();
            System.exit(0);
        });
        popup.add(exitItem);
        
        return popup;
    }
    
    /**
     * Create tray icon image
     */
    private Image createTrayImage() {
        // Create a simple icon (in production, load from resources)
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        // Draw a simple icon
        g.setColor(new Color(52, 152, 219)); // Blue
        g.fillRoundRect(0, 0, size, size, 4, 4);
        g.setColor(Color.WHITE);
        g.fillRect(4, 6, 8, 2);
        g.fillRect(4, 10, 8, 2);
        
        g.dispose();
        return image;
    }
    
    /**
     * Minimize application to system tray
     */
    public void minimizeToTray() {
        if (!traySupported) {
            log.warn("Cannot minimize to tray - not supported");
            return;
        }
        
        Platform.runLater(() -> {
            primaryStage.hide();
            log.debug("Application minimized to system tray");
            
            // Show notification
            if (trayIcon != null) {
                trayIcon.displayMessage(
                    "Developer Workday",
                    "Application minimized to system tray",
                    TrayIcon.MessageType.INFO
                );
            }
        });
    }
    
    /**
     * Restore window from system tray
     */
    public void restoreWindow() {
        Platform.runLater(() -> {
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();
            log.debug("Application restored from system tray");
        });
    }
    
    /**
     * Remove tray icon
     */
    public void removeTrayIcon() {
        if (trayIcon != null && traySupported) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
                log.debug("Tray icon removed");
            } catch (Exception e) {
                log.error("Failed to remove tray icon", e);
            }
        }
    }
    
    /**
     * Check if system tray is supported
     */
    public boolean isTraySupported() {
        return traySupported;
    }
    
    /**
     * Display notification via tray icon
     */
    public void showNotification(String title, String message, TrayIcon.MessageType type) {
        if (trayIcon != null && traySupported) {
            trayIcon.displayMessage(title, message, type);
        }
    }
}
