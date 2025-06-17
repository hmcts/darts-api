package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransientObjectDirectoryRepository extends JpaRepository<TransientObjectDirectoryEntity, Long> {

    @Query("""
        SELECT tod FROM TransformedMediaEntity tm, TransientObjectDirectoryEntity tod
        WHERE tod.transformedMedia = tm
        and tm.id = :transformedMediaId
        """)
    List<TransientObjectDirectoryEntity> findByTransformedMediaId(Integer transformedMediaId);

    @Query("""
        SELECT tod FROM TransientObjectDirectoryEntity tod
        LEFT JOIN FETCH tod.transformedMedia         
        WHERE tod.status = :status
        """)
    //Join required to ensure transient media is loaded into session
    List<TransientObjectDirectoryEntity> findByStatus(ObjectRecordStatusEntity status, Limit limit);


    @Query("""
               select tod from TransientObjectDirectoryEntity tod
               LEFT JOIN tod.transformedMedia tm
               where tm is null or tm.expiryTime < :maxExpiryTime 
               and tod.status.id = :statusId 
        """)
    List<TransientObjectDirectoryEntity> findByTransformedMediaIsNullOrExpirtyBeforeMaxExpiryTime(
        OffsetDateTime maxExpiryTime, Integer statusId, Limit limit);
}
