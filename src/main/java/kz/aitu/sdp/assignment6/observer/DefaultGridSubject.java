package kz.aitu.sdp.assignment6.observer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe implementation of {@link GridSubject} using fine-grained concurrency.
 *
 * Uses {@link ConcurrentHashMap} with {@link CopyOnWriteArrayList} per event type.
 * This guarantees:
 * 1. Safe concurrent event publication across producer threads without coarse locking.
 * 2. Dynamic subscription and unsubscription at runtime, including directly from within an observer's onEvent handler.
 * 3. Lock-free iteration during event dispatch.
 */
public class DefaultGridSubject implements GridSubject<GridEvent> {

    private final String subjectName;
    private final ConcurrentMap<GridEventType, CopyOnWriteArrayList<GridObserver<GridEvent>>> registry;
    private final AtomicLong totalDispatchedEvents;

    public DefaultGridSubject(String subjectName) {
        this.subjectName = Objects.requireNonNull(subjectName, "subjectName cannot be null");
        this.registry = new ConcurrentHashMap<>();
        this.totalDispatchedEvents = new AtomicLong(0);
    }

    @Override
    public void subscribe(GridEventType type, GridObserver<GridEvent> observer) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(observer, "observer cannot be null");
        registry.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).addIfAbsent(observer);
    }

    @Override
    public void unsubscribe(GridEventType type, GridObserver<GridEvent> observer) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(observer, "observer cannot be null");
        CopyOnWriteArrayList<GridObserver<GridEvent>> observers = registry.get(type);
        if (observers != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void notifyObservers(GridEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        List<GridObserver<GridEvent>> observers = registry.get(event.getType());
        if (observers != null && !observers.isEmpty()) {
            for (GridObserver<GridEvent> observer : observers) {
                observer.onEvent(event);
            }
        }
        totalDispatchedEvents.incrementAndGet();
    }

    @Override
    public int getObserverCount(GridEventType type) {
        List<GridObserver<GridEvent>> observers = registry.get(type);
        return observers != null ? observers.size() : 0;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public long getTotalDispatchedEvents() {
        return totalDispatchedEvents.get();
    }
}
