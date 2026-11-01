package kz.aitu.sdp.assignment6.observer;

/**
 * Generic subject (observable) interface allowing observers to subscribe and unsubscribe
 * dynamically per event type.
 *
 * @param <E> the type of grid event published
 */
public interface GridSubject<E extends GridEvent> {

    /**
     * Subscribes an observer to receive events of a specific type.
     */
    void subscribe(GridEventType type, GridObserver<E> observer);

    /**
     * Unsubscribes an observer from events of a specific type.
     */
    void unsubscribe(GridEventType type, GridObserver<E> observer);

    /**
     * Broadcasts an event to all observers registered for the event's type.
     */
    void notifyObservers(E event);

    /**
     * Returns the count of registered observers for a specific event type.
     */
    int getObserverCount(GridEventType type);
}
