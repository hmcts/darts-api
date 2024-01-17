package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransientObjectDirectoryRepository extends JpaRepository<TransientObjectDirectoryEntity, Integer> {

    @Query("""
        SELECT tod FROM TransformedMediaEntity tm, TransientObjectDirectoryEntity tod
        WHERE tod.transformedMedia = tm
        and tm.id = :transformedMediaId
        """)
    List<TransientObjectDirectoryEntity> findByTransformedMediaId(Integer transformedMediaId);


    List<TransientObjectDirectoryEntity> findByTransformedMediaIdIn(List<Integer> transformedMediaIds);


    @Query("""
        SELECT tod FROM MediaRequestEntity mr, TransformedMediaEntity tm, TransientObjectDirectoryEntity tod
        WHERE tod.transformedMedia = tm
        AND tm.mediaRequest = mr
        and mr.id = :mediaRequestId
        """)
    Optional<TransientObjectDirectoryEntity> findByMediaRequestId(Integer mediaRequestId);

    @Query("""
        SELECT tod FROM MediaRequestEntity mr, TransformedMediaEntity tm, TransientObjectDirectoryEntity tod
        WHERE tod.transformedMedia = tm
        AND tm.mediaRequest = mr
        and mr.id in :mediaRequestIds
        """)
    List<TransientObjectDirectoryEntity> findByMediaRequestIds(List<Integer> mediaRequestIds);

    List<TransientObjectDirectoryEntity> findByStatus(ObjectRecordStatusEntity status);

}
