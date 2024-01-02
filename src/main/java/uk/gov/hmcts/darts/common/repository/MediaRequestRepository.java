package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRequestRepository extends JpaRepository<MediaRequestEntity, Integer> {

    Optional<MediaRequestEntity> findTopByStatusOrderByCreatedDateTimeAsc(MediaRequestStatus status);

    @Query("""
        SELECT count(distinct(tm.id)) FROM MediaRequestEntity mr, TransformedMediaEntity tm
        WHERE tm.mediaRequest = mr
        AND mr.requestor.id = :userId
        AND tm.lastAccessed is null
        AND mr.status = :status
        """)
    long countTransformedEntitiesByRequestorIdAndStatusNotAccessed(Integer userId, MediaRequestStatus status);


    @Query("""
        SELECT distinct(mr.id) FROM MediaRequestEntity mr, TransformedMediaEntity tm
        WHERE tm.mediaRequest = mr
        AND tm.lastAccessed < :lastAccessedDateTime
        AND mr.status = :status
        """)
    List<Integer> findAllIdsByLastAccessedTimeBeforeAndStatus(OffsetDateTime lastAccessedDateTime, MediaRequestStatus status);

    @Query("""
        SELECT distinct(mr.id) FROM MediaRequestEntity mr, TransformedMediaEntity tm
        WHERE tm.mediaRequest = mr
        AND mr.createdDateTime < :createdDateTime
        AND mr.status <> :status
        AND tm.lastAccessed IS NULL
        """)
    List<Integer> findAllByCreatedDateTimeBeforeAndStatusNotAndLastAccessedDateTimeIsNull(OffsetDateTime createdDateTime, MediaRequestStatus status);

}
