package net.talaatharb.workday.config;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.facade.*;
import net.talaatharb.workday.mapper.CategoryMapper;
import net.talaatharb.workday.mapper.SubtaskMapper;
import net.talaatharb.workday.mapper.TaskMapper;
import net.talaatharb.workday.repository.*;
import net.talaatharb.workday.service.*;
import org.mapdb.DB;

/**
 * Application initializer that configures and registers all beans in the ApplicationContext.
 * This class follows the dependency order: Database → Repositories → Events → Services → Facades.
 */
@Slf4j
public class ApplicationInitializer {

    private final ApplicationContext context;
    private final DatabaseConfig databaseConfig;

    public ApplicationInitializer() {
        this.context = ApplicationContext.getInstance();
        this.databaseConfig = new DatabaseConfig();
    }

    /**
     * Initialize the application with default database configuration.
     * This is the main entry point for application startup.
     */
    public void initialize() {
        log.info("Starting application initialization...");

        try {
            // Step 1: Initialize database
            initializeDatabase();

            // Step 2: Initialize repositories
            initializeRepositories();

            // Step 3: Initialize event system
            initializeEventSystem();

            // Step 4: Initialize mappers
            initializeMappers();

            // Step 5: Initialize services
            initializeServices();

            // Step 6: Initialize facades
            initializeFacades();

            log.info("Application initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize application", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }

    /**
     * Initialize the application with a custom database (useful for testing).
     */
    public void initializeWithDatabase(DB customDatabase) {
        log.info("Starting application initialization with custom database...");

        try {
            // Register custom database
            context.registerBean(DB.class, customDatabase);
            context.registerBean(DatabaseConfig.class, databaseConfig);

            // Initialize remaining components
            initializeRepositories();
            initializeEventSystem();
            initializeMappers();
            initializeServices();
            initializeFacades();

            log.info("Application initialization with custom database completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize application with custom database", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }

    /**
     * Step 1: Initialize database configuration and connection.
     */
    private void initializeDatabase() {
        log.debug("Initializing database...");

        databaseConfig.initialize();
        DB database = databaseConfig.getDatabase();

        context.registerBean(DB.class, database);
        context.registerBean(DatabaseConfig.class, databaseConfig);

        log.debug("Database initialized and registered");
    }

    /**
     * Step 2: Initialize all repository beans.
     */
    private void initializeRepositories() {
        log.debug("Initializing repositories...");

        DB database = context.getBean(DB.class);

        // Create and register repositories
        TaskRepository taskRepository = new TaskRepository(database);
        CategoryRepository categoryRepository = new CategoryRepository(database);
        PreferencesRepository preferencesRepository = new PreferencesRepository(database);
        EventStoreRepository eventStoreRepository = new EventStoreRepository(database);

        context.registerBean(TaskRepository.class, taskRepository);
        context.registerBean(CategoryRepository.class, categoryRepository);
        context.registerBean(PreferencesRepository.class, preferencesRepository);
        context.registerBean(EventStoreRepository.class, eventStoreRepository);

        log.debug("Repositories initialized and registered");
    }

    /**
     * Step 3: Initialize event system (EventLogger and EventDispatcher).
     */
    private void initializeEventSystem() {
        log.debug("Initializing event system...");

        // Create event logger
        EventLogger eventLogger = new EventLogger(1000);
        context.registerBean(EventLogger.class, eventLogger);

        // Create event dispatcher with event logger
        EventDispatcher eventDispatcher = new EventDispatcher(eventLogger);
        context.registerBean(EventDispatcher.class, eventDispatcher);

        log.debug("Event system initialized and registered");
    }

    /**
     * Step 4: Initialize mapper beans.
     */
    private void initializeMappers() {
        log.debug("Initializing mappers...");

        // Mappers are MapStruct-generated singletons
        CategoryMapper categoryMapper = CategoryMapper.INSTANCE;
        SubtaskMapper subtaskMapper = SubtaskMapper.INSTANCE;
        TaskMapper taskMapper = TaskMapper.INSTANCE;

        context.registerBean(CategoryMapper.class, categoryMapper);
        context.registerBean(SubtaskMapper.class, subtaskMapper);
        context.registerBean(TaskMapper.class, taskMapper);

        log.debug("Mappers initialized and registered");
    }

    /**
     * Step 5: Initialize all service beans.
     */
    private void initializeServices() {
        log.debug("Initializing services...");

        // Get dependencies from context
        TaskRepository taskRepository = context.getBean(TaskRepository.class);
        CategoryRepository categoryRepository = context.getBean(CategoryRepository.class);
        PreferencesRepository preferencesRepository = context.getBean(PreferencesRepository.class);
        EventStoreRepository eventStoreRepository = context.getBean(EventStoreRepository.class);
        EventDispatcher eventDispatcher = context.getBean(EventDispatcher.class);

        // Create services with their dependencies
        TaskService taskService = new TaskService(taskRepository, eventDispatcher);
        CategoryService categoryService = new CategoryService(categoryRepository, taskRepository, eventDispatcher);
        PreferencesService preferencesService = new PreferencesService(preferencesRepository, eventDispatcher);
        ReminderService reminderService = new ReminderService(eventDispatcher);
        NotificationService notificationService = new NotificationService(null); // TrayIcon can be set later
        StatisticsService statisticsService = new StatisticsService(taskRepository);
        TagService tagService = new TagService(taskRepository);
        QuickActionsService quickActionsService = new QuickActionsService();
        FocusModeService focusModeService = new FocusModeService(eventDispatcher);
        WeeklyReviewService weeklyReviewService = new WeeklyReviewService(taskRepository, eventDispatcher);
        UpdateCheckService updateCheckService = new UpdateCheckService(preferencesService);

        // Get database path for attachment service
        DatabaseConfig dbConfig = context.getBean(DatabaseConfig.class);
        String appDataDir = dbConfig.getDatabasePath().getParent().toString();
        AttachmentService attachmentService = new AttachmentService(appDataDir);

        // Services that depend on other services (created after initial services)
        TimeTrackingService timeTrackingService = new TimeTrackingService(eventDispatcher, taskService);
        DataExportService dataExportService = new DataExportService(taskService, categoryService, preferencesService);
        DataImportService dataImportService = new DataImportService(taskService, categoryService, preferencesService);

        // Register services
        context.registerBean(TaskService.class, taskService);
        context.registerBean(CategoryService.class, categoryService);
        context.registerBean(PreferencesService.class, preferencesService);
        context.registerBean(ReminderService.class, reminderService);
        context.registerBean(NotificationService.class, notificationService);
        context.registerBean(StatisticsService.class, statisticsService);
        context.registerBean(TagService.class, tagService);
        context.registerBean(TimeTrackingService.class, timeTrackingService);
        context.registerBean(QuickActionsService.class, quickActionsService);
        context.registerBean(FocusModeService.class, focusModeService);
        context.registerBean(WeeklyReviewService.class, weeklyReviewService);
        context.registerBean(UpdateCheckService.class, updateCheckService);
        context.registerBean(AttachmentService.class, attachmentService);
        context.registerBean(DataExportService.class, dataExportService);
        context.registerBean(DataImportService.class, dataImportService);

        log.debug("Services initialized and registered");
    }

    /**
     * Step 6: Initialize all facade beans.
     */
    private void initializeFacades() {
        log.debug("Initializing facades...");

        // Get services from context
        TaskService taskService = context.getBean(TaskService.class);
        CategoryService categoryService = context.getBean(CategoryService.class);
        PreferencesService preferencesService = context.getBean(PreferencesService.class);
        ReminderService reminderService = context.getBean(ReminderService.class);
        NotificationService notificationService = context.getBean(NotificationService.class);
        StatisticsService statisticsService = context.getBean(StatisticsService.class);
        FocusModeService focusModeService = context.getBean(FocusModeService.class);
        WeeklyReviewService weeklyReviewService = context.getBean(WeeklyReviewService.class);
        UpdateCheckService updateCheckService = context.getBean(UpdateCheckService.class);
        EventDispatcher eventDispatcher = context.getBean(EventDispatcher.class);

        // Get mappers from context
        TaskMapper taskMapper = context.getBean(TaskMapper.class);

        // Create facades with their dependencies
        TaskFacade taskFacade = new TaskFacade(taskService, reminderService);
        CategoryFacade categoryFacade = new CategoryFacade(categoryService, taskService);
        PreferencesFacade preferencesFacade = new PreferencesFacade(preferencesService);
        CalendarFacade calendarFacade = new CalendarFacade(taskService);
        FocusModeFacade focusModeFacade = new FocusModeFacade(focusModeService, notificationService, eventDispatcher);
        WeeklyReviewFacade weeklyReviewFacade = new WeeklyReviewFacade(weeklyReviewService, taskService, taskMapper);
        UpdateCheckFacade updateCheckFacade = new UpdateCheckFacade(updateCheckService);

        // Register facades
        context.registerBean(TaskFacade.class, taskFacade);
        context.registerBean(CategoryFacade.class, categoryFacade);
        context.registerBean(PreferencesFacade.class, preferencesFacade);
        context.registerBean(CalendarFacade.class, calendarFacade);
        context.registerBean(FocusModeFacade.class, focusModeFacade);
        context.registerBean(WeeklyReviewFacade.class, weeklyReviewFacade);
        context.registerBean(UpdateCheckFacade.class, updateCheckFacade);

        log.debug("Facades initialized and registered");
    }

    /**
     * Shutdown the application gracefully, closing database connections.
     */
    public void shutdown() {
        log.info("Shutting down application...");

        try {
            // Close database
            if (context.hasBean(DatabaseConfig.class)) {
                DatabaseConfig dbConfig = context.getBean(DatabaseConfig.class);
                dbConfig.close();
            }

            // Clear application context
            context.clear();

            log.info("Application shutdown completed");
        } catch (Exception e) {
            log.error("Error during application shutdown", e);
        }
    }
}
