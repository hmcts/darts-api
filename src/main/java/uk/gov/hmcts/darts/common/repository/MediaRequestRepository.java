package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

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
        SELECT mr
        FROM MediaRequestEntity mr
        WHERE mr.hearing = :hearing
        AND mr.requestor = :userAccount
        AND mr.startTime = :startTime
        AND mr.endTime = :endTime
        AND mr.requestType = :requestType
        AND mr.status IN :requestStatuses
        """)
    Optional<MediaRequestEntity> findDuplicateUserMediaRequests(HearingEntity hearing, UserAccountEntity userAccount,
                                                                OffsetDateTime startTime, OffsetDateTime endTime,
                                                                AudioRequestType requestType, List<MediaRequestStatus> requestStatuses);

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
        AND tm.createdDateTime < :createdDateTime
        AND mr.status <> :status
        AND tm.lastAccessed IS NULL
        """)
    List<Integer> findAllByCreatedDateTimeBeforeAndStatusNotAndLastAccessedDateTimeIsNull(OffsetDateTime createdDateTime, MediaRequestStatus status);
}
