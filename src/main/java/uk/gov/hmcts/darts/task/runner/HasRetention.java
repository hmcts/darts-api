package uk.gov.hmcts.darts.task.runner;

import java.time.OffsetDateTime;

/**
 * Interface for entities that have an retention details.
 * Used with generic methods that need to work with entities that have an retention details
 */
public interface HasRetention {
    OffsetDateTime getRetainUntilTs();
}
