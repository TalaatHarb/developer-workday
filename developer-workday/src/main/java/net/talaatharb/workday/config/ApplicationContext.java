package net.talaatharb.workday.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple application context for dependency injection.
 * Manages singleton instances of application components.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationContext {
    
    private static final ApplicationContext INSTANCE = new ApplicationContext();
    private final Map<Class<?>, Object> beans = new HashMap<>();
    
    public static ApplicationContext getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a bean instance
     */
    public <T> void registerBean(Class<T> type, T instance) {
        log.debug("Registering bean: {}", type.getName());
        beans.put(type, instance);
    }
    
    /**
     * Register a bean with lazy initialization
     */
    public <T> void registerLazyBean(Class<T> type, Supplier<T> supplier) {
        log.debug("Registering lazy bean: {}", type.getName());
        beans.put(type, supplier.get());
    }
    
    /**
     * Get a bean instance
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        Object bean = beans.get(type);
        if (bean == null) {
            throw new IllegalStateException("No bean registered for type: " + type.getName());
        }
        return (T) bean;
    }
    
    /**
     * Get an optional bean instance
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getBeanOptional(Class<T> type) {
        return Optional.ofNullable((T) beans.get(type));
    }
    
    /**
     * Check if a bean is registered
     */
    public boolean hasBean(Class<?> type) {
        return beans.containsKey(type);
    }
    
    /**
     * Clear all registered beans (for testing)
     */
    public void clear() {
        log.debug("Clearing application context");
        beans.clear();
    }
}
