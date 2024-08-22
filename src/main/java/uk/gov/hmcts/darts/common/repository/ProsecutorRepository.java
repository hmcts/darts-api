package uk.gov.hmcts.darts.common.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;

@Repository
public interface ProsecutorRepository extends JpaRepository<ProsecutorEntity, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO prosecutor (prn_id, cas_id, prosecutor_name, created_ts, created_by, last_modified_ts, last_modified_by) " +
        "VALUES (:#{#entity.id}, :#{#entity.courtCase.id}, :#{#entity.name}, " +
        ":#{#entity.createdDateTime}, :#{#entity.createdBy.id}, " +
        ":#{#entity.lastModifiedDateTime}, :#{#entity.lastModifiedBy.id}) " +
        "ON CONFLICT (prn_id) DO UPDATE SET " +
        "cas_id = :#{#entity.courtCase.id}, " +
        "prosecutor_name = :#{#entity.name}, " +
        "last_modified_ts = :#{#entity.lastModifiedDateTime}, " +
        "last_modified_by = :#{#entity.lastModifiedBy.id} " +
        "RETURNING *",
        nativeQuery = true)
    ProsecutorEntity insertOrUpdate(@Param("entity") ProsecutorEntity entity);

}
