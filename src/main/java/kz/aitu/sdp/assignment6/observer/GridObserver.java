package kz.aitu.sdp.assignment6.observer;

/**
 * Generic observer interface for handling incoming grid events.
 *
 * @param <E> the type of grid event observed
 */
public interface GridObserver<E extends GridEvent> {

    /**
     * Invoked when an event of the subscribed type is dispatched by a subject.
     *
     * @param event the immutable event payload
     */
    void onEvent(E event);

    /**
     * Unique identifier of this observer for logging and lifecycle tracking.
     */
    String getObserverId();
}
