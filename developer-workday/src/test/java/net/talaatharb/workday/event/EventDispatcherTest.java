package net.talaatharb.workday.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventDispatcherTest {
    
    private EventDispatcher dispatcher;
    private EventLogger logger;
    
    @BeforeEach
    void setUp() {
        logger = new EventLogger();
        dispatcher = new EventDispatcher(logger);
    }
    
    @Test
    void testSubscribeAndPublish() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<TestEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(TestEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        TestEvent event = new TestEvent("Test message");
        dispatcher.publish(event);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        assertEquals(event.getEventId(), receivedEvents.get(0).getEventId());
    }
    
    @Test
    void testMultipleListeners() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        
        dispatcher.subscribe(TestEvent.class, event -> latch.countDown());
        dispatcher.subscribe(TestEvent.class, event -> latch.countDown());
        
        dispatcher.publish(new TestEvent("Test"));
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(2, dispatcher.getListenerCount(TestEvent.class));
    }
    
    @Test
    void testUnsubscribe() {
        EventListener<TestEvent> listener = event -> {};
        
        dispatcher.subscribe(TestEvent.class, listener);
        assertEquals(1, dispatcher.getListenerCount(TestEvent.class));
        
        dispatcher.unsubscribe(TestEvent.class, listener);
        assertEquals(0, dispatcher.getListenerCount(TestEvent.class));
    }
    
    @Test
    void testEventLogging() {
        TestEvent event = new TestEvent("Logged event");
        dispatcher.publish(event);
        
        assertEquals(1, logger.getEventCount());
        List<TestEvent> loggedEvents = logger.getEventsByType(TestEvent.class);
        assertEquals(1, loggedEvents.size());
        assertEquals(event.getEventId(), loggedEvents.get(0).getEventId());
    }
    
    @Test
    void testClearListeners() {
        dispatcher.subscribe(TestEvent.class, event -> {});
        dispatcher.subscribe(TestEvent.class, event -> {});
        assertEquals(2, dispatcher.getListenerCount(TestEvent.class));
        
        dispatcher.clearAllListeners();
        assertEquals(0, dispatcher.getListenerCount(TestEvent.class));
    }
    
    static class TestEvent extends Event {
        private final String message;
        
        TestEvent(String message) {
            super();
            this.message = message;
        }
        
        @Override
        public String getEventDetails() {
            return "TestEvent: " + message;
        }
    }
}
