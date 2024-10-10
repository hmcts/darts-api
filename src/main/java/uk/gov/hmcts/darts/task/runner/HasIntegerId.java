package uk.gov.hmcts.darts.task.runner;

/**
 * Interface for entities that have an integer ID.
 * Used with generic methods that need to work with entities that have an integer ID.
 */
public interface HasIntegerId {
    Integer getId();
}
