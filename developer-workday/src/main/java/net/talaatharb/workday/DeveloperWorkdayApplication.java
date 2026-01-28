package net.talaatharb.workday;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.config.ApplicationInitializer;

@Slf4j
public class DeveloperWorkdayApplication{
	public static void main(String[] args) {
		log.info("UI Application Starting");

		// Initialize application context and register all beans
		ApplicationInitializer initializer = new ApplicationInitializer();
		initializer.initialize();

		// Add shutdown hook to close database gracefully
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Shutdown hook triggered");
			initializer.shutdown();
		}));

		// Launch JavaFX application
		Application.launch(JavafxApplication.class, args);
	}
}