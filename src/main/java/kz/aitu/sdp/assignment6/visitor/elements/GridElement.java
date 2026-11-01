package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

/**
 * Common abstraction for all nodes in the Energy Grid composite tree.
 * Holds component identity and enforces the double-dispatch mechanism.
 */
public interface GridElement {

    /**
     * Entry point for double dispatch: dispatches execution back to the visitor.
     *
     * @param visitor the visitor operating on this grid element
     * @param <R> the return type of the visitor's operation
     * @return the result computed by the visitor
     */
    <R> R accept(GridVisitor<R> visitor);

    /**
     * Unique identifier of this grid component (e.g. SUB-01, LINE-101, RES-001).
     */
    String getId();

    /**
     * Human-readable label or designation of this component.
     */
    String getName();
}
