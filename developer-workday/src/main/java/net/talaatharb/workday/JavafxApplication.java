package net.talaatharb.workday;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.talaatharb.workday.config.ApplicationContext;
import net.talaatharb.workday.facade.PreferencesFacade;
import net.talaatharb.workday.facade.UpdateCheckFacade;
import net.talaatharb.workday.model.UpdateInfo;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.utils.ThemeManager;

public class JavafxApplication extends Application {

	private static final int HEIGHT = 50;
	private static final String MAIN_FXML = "ui/MainWindow.fxml";
	private static final String ICON_FILE = "ui/logo.jpg";
	private static final String TITLE = "Developer Workday";
	private static final int WIDTH = 800;

	@Override
	public void start(Stage primaryStage) throws Exception {
		final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(MAIN_FXML));
		final Parent root = fxmlLoader.load();

		final Image icon = new Image(getClass().getResourceAsStream(ICON_FILE));
		final Scene scene = new Scene(root, WIDTH, HEIGHT);
		
		// Load theme from preferences
		loadAndApplyTheme(scene);

		primaryStage.setScene(scene);
		primaryStage.setTitle(TITLE);
		primaryStage.getIcons().add(icon);
		primaryStage.show();
		
		// Check for updates on startup (in background)
		checkForUpdatesOnStartup();
	}

	private void loadAndApplyTheme(Scene scene) {
		try {
			// Register scene with theme manager
			ThemeManager themeManager = ThemeManager.getInstance();
			themeManager.registerScene(scene);
			
			// Try to load theme from preferences
			ApplicationContext context = ApplicationContext.getInstance();
			if (context.hasBean(PreferencesFacade.class)) {
				PreferencesFacade preferencesFacade = context.getBean(PreferencesFacade.class);
				UserPreferences prefs = preferencesFacade.getPreferences();
				themeManager.applyTheme(prefs.getTheme());
			} else {
				// Default to light theme if preferences not available
				themeManager.applyTheme("light");
			}
		} catch (Exception e) {
			// Fallback to light theme on error
			ThemeManager.getInstance().applyTheme("light");
		}
	}
	
	/**
	 * Check for updates on startup (if enabled in preferences)
	 */
	private void checkForUpdatesOnStartup() {
		new Thread(() -> {
			try {
				ApplicationContext context = ApplicationContext.getInstance();
				if (context.hasBean(UpdateCheckFacade.class)) {
					UpdateCheckFacade updateCheckFacade = context.getBean(UpdateCheckFacade.class);
					UpdateInfo updateInfo = updateCheckFacade.checkForUpdatesIfEnabled();
					
					// If update available, show notification on UI thread
					if (updateInfo != null && updateInfo.isUpdateAvailable()) {
						Platform.runLater(() -> showUpdateNotification(updateInfo));
					}
				}
			} catch (Exception e) {
				// Silently fail - update check is not critical
			}
		}).start();
	}
	
	/**
	 * Show update notification
	 */
	private void showUpdateNotification(UpdateInfo updateInfo) {
		javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
			javafx.scene.control.Alert.AlertType.INFORMATION
		);
		alert.setTitle("Update Available");
		alert.setHeaderText(String.format("Version %s is available!", updateInfo.getLatestVersion()));
		alert.setContentText(String.format(
			"A new version of Developer Workday is available.\n\n" +
			"Current version: %s\n" +
			"Latest version: %s\n\n" +
			"You can check for updates from the Help menu.",
			updateInfo.getCurrentVersion(),
			updateInfo.getLatestVersion()
		));
		alert.show();
	}

	@Override
	public void stop() throws Exception {
		Platform.exit();
	}

}
