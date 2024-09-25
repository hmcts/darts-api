package uk.gov.hmcts.darts.task.runner;

import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;


public interface SoftDeleteRepository<T extends SoftDelete, I> extends CrudRepository<T, I> {

    default void softDeleteAll(List<T> entities, UserAccountEntity userAccount) {
        entities.forEach(t -> t.markAsDeleted(userAccount));
        saveAll(entities);
    }

    default void softDelete(T entity, UserAccountEntity userAccount) {
        entity.markAsDeleted(userAccount);
        save(entity);
    }
}
