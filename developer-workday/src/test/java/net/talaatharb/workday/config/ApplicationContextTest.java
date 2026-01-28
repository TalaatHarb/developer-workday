package net.talaatharb.workday.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationContextTest {
    
    private ApplicationContext context;
    
    @BeforeEach
    void setUp() {
        context = ApplicationContext.getInstance();
        context.clear();
    }
    
    @AfterEach
    void tearDown() {
        context.clear();
    }
    
    @Test
    void testRegisterAndGetBean() {
        TestService testBean = new TestService();
        context.registerBean(TestService.class, testBean);
        
        TestService retrievedBean = context.getBean(TestService.class);
        assertSame(testBean, retrievedBean);
    }
    
    @Test
    void testRegisterBeanWithSupplier() {
        context.registerLazyBean(TestService.class, () -> new TestService());
        
        TestService retrievedBean = context.getBean(TestService.class);
        assertNotNull(retrievedBean);
    }
    
    @Test
    void testGetBeanThrowsExceptionWhenNotRegistered() {
        assertThrows(IllegalStateException.class, () -> context.getBean(TestService.class));
    }
    
    @Test
    void testGetBeanOptional() {
        context.registerBean(TestService.class, new TestService());
        
        assertTrue(context.getBeanOptional(TestService.class).isPresent());
        assertFalse(context.getBeanOptional(AnotherService.class).isPresent());
    }
    
    @Test
    void testHasBean() {
        assertFalse(context.hasBean(TestService.class));
        
        context.registerBean(TestService.class, new TestService());
        assertTrue(context.hasBean(TestService.class));
    }
    
    @Test
    void testSingleton() {
        ApplicationContext instance1 = ApplicationContext.getInstance();
        ApplicationContext instance2 = ApplicationContext.getInstance();
        
        assertSame(instance1, instance2);
    }
    
    private static class TestService {}
    private static class AnotherService {}
}
