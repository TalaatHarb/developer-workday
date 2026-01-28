# Application Initialization Guide

## Overview

The Developer Workday application now has a comprehensive startup initialization system that ensures all beans (components) are properly registered in the `ApplicationContext` before the application starts.

## Architecture

The initialization follows a dependency-ordered approach:

```
1. Database (MapDB)
   ↓
2. Repositories (TaskRepository, CategoryRepository, etc.)
   ↓
3. Event System (EventLogger, EventDispatcher)
   ↓
4. Mappers (TaskMapper, CategoryMapper, SubtaskMapper)
   ↓
5. Services (TaskService, CategoryService, etc.)
   ↓
6. Facades (TaskFacade, CategoryFacade, etc.)
```

## Key Components

### ApplicationContext
- **Location**: `net.talaatharb.workday.config.ApplicationContext`
- **Purpose**: Simple dependency injection container that manages singleton beans
- **Methods**:
  - `registerBean(Class<T>, T)`: Register a bean instance
  - `getBean(Class<T>)`: Retrieve a registered bean
  - `hasBean(Class<?>)`: Check if a bean exists
  - `clear()`: Clear all beans (for testing)

### ApplicationInitializer
- **Location**: `net.talaatharb.workday.config.ApplicationInitializer`
- **Purpose**: Bootstrap all application components in correct dependency order
- **Methods**:
  - `initialize()`: Main entry point - initializes everything with default database
  - `initializeWithDatabase(DB)`: Alternative for testing with custom/in-memory database
  - `shutdown()`: Gracefully closes database and cleans up resources

### DatabaseConfig
- **Location**: `net.talaatharb.workday.config.DatabaseConfig`
- **Purpose**: Manages MapDB database lifecycle and configuration
- **Features**:
  - Auto-creates database directory based on OS (Windows AppData, Mac Library, Linux .local)
  - Provides transaction support
  - Graceful shutdown handling

## How It Works

### 1. Application Startup Flow

```java
// In DeveloperWorkdayApplication.main()
ApplicationInitializer initializer = new ApplicationInitializer();
initializer.initialize();

// Beans are now available via:
ApplicationContext.getInstance().getBean(TaskFacade.class)
```

### 2. Registered Components

#### Repositories
- `TaskRepository`
- `CategoryRepository`
- `PreferencesRepository`
- `EventStoreRepository`

#### Services
- `TaskService` - Core task management
- `CategoryService` - Category management
- `PreferencesService` - User preferences
- `ReminderService` - Task reminders
- `NotificationService` - System notifications
- `StatisticsService` - Productivity stats
- `TagService` - Tag management
- `TimeTrackingService` - Time tracking
- `QuickActionsService` - Command palette
- `FocusModeService` - Do not disturb mode
- `WeeklyReviewService` - Weekly reviews
- `UpdateCheckService` - Update checking
- `AttachmentService` - File attachments
- `DataExportService` - Data export
- `DataImportService` - Data import

#### Facades
- `TaskFacade` - Simplified task operations for UI
- `CategoryFacade` - Simplified category operations for UI
- `PreferencesFacade` - Preferences operations for UI
- `CalendarFacade` - Calendar view operations
- `FocusModeFacade` - Focus mode operations
- `WeeklyReviewFacade` - Weekly review operations
- `UpdateCheckFacade` - Update check operations

#### Event System
- `EventLogger` - In-memory event audit log
- `EventDispatcher` - Pub/sub event system

#### Mappers
- `TaskMapper` - Task entity/DTO mapping
- `CategoryMapper` - Category entity/DTO mapping
- `SubtaskMapper` - Subtask entity/DTO mapping

### 3. Using Beans in Your Code

```java
// Get the application context
ApplicationContext context = ApplicationContext.getInstance();

// Retrieve beans
TaskFacade taskFacade = context.getBean(TaskFacade.class);
CategoryFacade categoryFacade = context.getBean(CategoryFacade.class);

// Use the facade
List<Task> todaysTasks = taskFacade.getTasksForToday();
```

### 4. Testing with Custom Database

For unit tests, you can use an in-memory database:

```java
@BeforeEach
void setUp() {
    ApplicationContext context = ApplicationContext.getInstance();
    context.clear(); // Clear any previous beans
    
    // Create in-memory database
    DB testDb = DatabaseConfig.inMemoryDatabase();
    
    // Initialize with custom database
    ApplicationInitializer initializer = new ApplicationInitializer();
    initializer.initializeWithDatabase(testDb);
}

@AfterEach
void tearDown() {
    ApplicationContext context = ApplicationContext.getInstance();
    if (context.hasBean(DB.class)) {
        context.getBean(DB.class).close();
    }
    context.clear();
}
```

## Database Location

The database is automatically created at:

- **Windows**: `%APPDATA%\developer-workday\developer-workday.db`
- **macOS**: `~/Library/Application Support/developer-workday/developer-workday.db`
- **Linux**: `~/.local/share/developer-workday/developer-workday.db`

## Shutdown Handling

The application registers a shutdown hook to ensure clean database closure:

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    initializer.shutdown();
}));
```

This ensures:
- Database transactions are committed
- File handles are released
- Resources are cleaned up properly

## Troubleshooting

### Issue: Beans not found
**Solution**: Ensure `ApplicationInitializer.initialize()` is called before accessing any beans.

### Issue: Database locked
**Solution**: Only one instance of the application can run at a time. Close other instances.

### Issue: Test failures with database
**Solution**: Use `initializeWithDatabase()` with in-memory database for tests, and always call `clear()` in `@AfterEach`.

## Adding New Components

To add a new service/facade:

1. **Create the class** with appropriate dependencies in constructor
2. **Update ApplicationInitializer**:
   ```java
   // In initializeServices() or initializeFacades()
   MyNewService myService = new MyNewService(dependency1, dependency2);
   context.registerBean(MyNewService.class, myService);
   ```
3. **Access it** via ApplicationContext:
   ```java
   MyNewService service = context.getBean(MyNewService.class);
   ```

## Benefits

✅ **Clean Dependency Management**: All dependencies are explicit and managed centrally
✅ **Testability**: Easy to swap implementations for testing
✅ **Lifecycle Management**: Proper initialization order and graceful shutdown
✅ **Type Safety**: Compile-time type checking with generics
✅ **Single Source of Truth**: One place to see all application components
✅ **No Magic**: Simple, understandable dependency injection without complex frameworks
