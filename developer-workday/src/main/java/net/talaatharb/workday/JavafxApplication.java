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

	@Override
	public void stop() throws Exception {
		Platform.exit();
	}

}
