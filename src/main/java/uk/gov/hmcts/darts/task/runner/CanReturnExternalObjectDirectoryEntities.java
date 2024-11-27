package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.List;

/**
 * Interface for entities that can return assocaited external object directory entities.
 * Used with generic methods that need to work with entities that can return associated external object directory entities
 */
public interface CanReturnExternalObjectDirectoryEntities {
    List<ExternalObjectDirectoryEntity> getExternalObjectDirectoryEntities();
}
