package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRequestRepository extends
    RevisionRepository<MediaRequestEntity, Integer, Long>,
    JpaRepository<MediaRequestEntity, Integer> {

    Optional<MediaRequestEntity> findTopByStatusOrderByLastModifiedDateTimeAsc(MediaRequestStatus status);

    @Transactional
    @Query(value = """
        UPDATE darts.media_request
        SET request_status = 'PROCESSING',
        last_modified_ts = current_timestamp,
        last_modified_by = :userModifiedId
        WHERE mer_id IN (
          SELECT mr2.mer_id
          FROM darts.media_request mr2
          WHERE mr2.request_status = 'OPEN'
          AND mr2.mer_id NOT IN ( :mediaRequestIdsToIgnore )
          ORDER BY mr2.last_modified_ts ASC
          LIMIT 1
          )
        
        RETURNING mer_id
        """, nativeQuery = true)
    Integer updateAndRetrieveMediaRequestToProcessing(int userModifiedId, List<Integer> mediaRequestIdsToIgnore);

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


    @Modifying
    @Query("""
        UPDATE MediaRequestEntity  mr
        SET mr.status = MediaRequestStatus.FAILED
        WHERE mr.status = MediaRequestStatus.PROCESSING
        AND mr.lastModifiedDateTime < :maxStuckTime
        """
    )
    @Transactional
    void cleanupStuckRequests(OffsetDateTime maxStuckTime);
}