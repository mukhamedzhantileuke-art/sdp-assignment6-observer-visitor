package kz.aitu.sdp.assignment6.concurrency;

import kz.aitu.sdp.assignment6.observer.DefaultGridSubject;
import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridEventType;
import kz.aitu.sdp.assignment6.observer.GridObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Concurrency & Stress Tests (Clean Code ch. 13)")
class ConcurrencyStressTest {

    @Test
    @DisplayName("Test 15 (Stress Test): 4 threads x 1,000 events with exact expected count = 4,000")
    void testStressConcurrentPublishingExactEventCount() throws InterruptedException {
        int threadCount = 4;
        int eventsPerThread = 1000;
        int totalExpectedEvents = threadCount * eventsPerThread; // Exactly 4,000

        DefaultGridSubject subject = new DefaultGridSubject("StressSubject");
        AtomicInteger processedCounter = new AtomicInteger(0);

        subject.subscribe(GridEventType.METER_READING, new GridObserver<>() {
            @Override
            public void onEvent(GridEvent event) {
                processedCounter.incrementAndGet();
            }

            @Override
            public String getObserverId() {
                return "STRESS-COLLECTOR";
            }
        });

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            pool.submit(() -> {
                try {
                    startGate.await(); // All threads begin simultaneously
                    for (int i = 0; i < eventsPerThread; i++) {
                        GridEvent event = GridEvent.of(
                                "EVT-" + threadIndex + "-" + i,
                                GridEventType.METER_READING,
                                "MTR-" + threadIndex,
                                "LINE-" + threadIndex,
                                5.5,
                                "Stress reading"
                        );
                        subject.notifyObservers(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startGate.countDown();

        boolean finishedInTime = endGate.await(10, TimeUnit.SECONDS);
        assertTrue(finishedInTime, "All producer threads should complete within 10 seconds");

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "Pool should shut down cleanly");

        // Verify exact count: 4 threads * 1,000 events = 4,000
        assertEquals(totalExpectedEvents, processedCounter.get(),
                "Listener must receive exactly 4,000 events without loss or duplication");
        assertEquals(totalExpectedEvents, subject.getTotalDispatchedEvents(),
                "Subject dispatched tally must equal 4,000");
    }

    @Test
    @DisplayName("Test 16: Dynamic subscription and unsubscription under concurrent publishing load")
    void testConcurrentSubscriptionAndUnsubscriptionUnderLoad() throws InterruptedException {
        DefaultGridSubject subject = new DefaultGridSubject("ConcurrentRegistrySubject");
        int publisherThreads = 4;
        int eventsPerPublisher = 500;
        ExecutorService pool = Executors.newFixedThreadPool(publisherThreads + 2);

        CountDownLatch doneLatch = new CountDownLatch(publisherThreads);

        // Publisher tasks
        for (int i = 0; i < publisherThreads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerPublisher; j++) {
                        subject.notifyObservers(GridEvent.of("ID", GridEventType.LOAD_SURGE, "SRC", "SUB", 100.0, "Msg"));
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Concurrent mutator task adding and removing observers simultaneously
        AtomicInteger dynamicEventsReceived = new AtomicInteger(0);
        pool.submit(() -> {
            for (int k = 0; k < 100; k++) {
                GridObserver<GridEvent> dynamicObserver = new GridObserver<>() {
                    @Override
                    public void onEvent(GridEvent event) {
                        dynamicEventsReceived.incrementAndGet();
                    }

                    @Override
                    public String getObserverId() {
                        return "DYNAMIC-" + Thread.currentThread().getId();
                    }
                };
                subject.subscribe(GridEventType.LOAD_SURGE, dynamicObserver);
                subject.unsubscribe(GridEventType.LOAD_SURGE, dynamicObserver);
            }
        });

        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "Producers should finish without deadlock or concurrency exceptions");

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Test 17: Simulation Engine achieves graceful shutdown with zero lingering threads")
    void testGracefulShutdownUnderLoad() {
        GridSimulationEngine engine = new GridSimulationEngine(3);

        CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            engine.submitProducer(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean cleanShutdown = engine.shutdownGracefully(3000);
        assertTrue(cleanShutdown, "Engine should shut down cleanly within timeout");
        assertTrue(engine.isShutdown(), "Engine must report shutdown state");
        assertTrue(engine.isTerminated(), "Engine must report terminated state");
    }
}
