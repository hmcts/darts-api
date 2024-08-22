package uk.gov.hmcts.darts.common.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;

@Repository
public interface DefendantRepository extends JpaRepository<DefendantEntity, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO " + DefendantEntity.TABLE_NAME +
        " (dfd_id, cas_id, defendant_name, created_ts, created_by, last_modified_ts, last_modified_by) " +
        "VALUES (" +
        ":#{#entity.id}, " +
        ":#{#entity.courtCase.id}, " +
        ":#{#entity.name}, " +
        ":#{#entity.createdDateTime}, " +
        ":#{#entity.createdBy.id}, " +
        ":#{#entity.lastModifiedDateTime}, " +
        ":#{#entity.lastModifiedBy.id}" +
        ") " +
        "ON CONFLICT (dfd_id) DO UPDATE SET " +
        "cas_id = :#{#entity.courtCase.id}, " +
        "defendant_name = :#{#entity.name}, " +
        "last_modified_ts = :#{#entity.lastModifiedDateTime}, " +
        "last_modified_by = :#{#entity.lastModifiedBy.id} " +
        "RETURNING *",
        nativeQuery = true)
    <S extends DefendantEntity> S save(@Param("entity") @NonNull S entity);

    @Override
    @Transactional
    default <S extends DefendantEntity> S saveAndFlush(@Param("entity") @NonNull S entity) {
        S savedEntity = save(entity);
        EntityManager entityManager = getEntityManager();
        entityManager.refresh(savedEntity);
        entityManager.flush();
        return savedEntity;
    }

    @PersistenceContext
    EntityManager getEntityManager();
}
