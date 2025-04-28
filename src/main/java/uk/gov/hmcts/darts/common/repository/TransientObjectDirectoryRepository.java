package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.util.List;

@Repository
public interface TransientObjectDirectoryRepository extends JpaRepository<TransientObjectDirectoryEntity, Long> {

    @Query("""
        SELECT tod FROM TransformedMediaEntity tm, TransientObjectDirectoryEntity tod
        WHERE tod.transformedMedia = tm
        and tm.id = :transformedMediaId
        """)
    List<TransientObjectDirectoryEntity> findByTransformedMediaId(Integer transformedMediaId);

    List<TransientObjectDirectoryEntity> findByStatus(ObjectRecordStatusEntity status, Limit limit);
}
