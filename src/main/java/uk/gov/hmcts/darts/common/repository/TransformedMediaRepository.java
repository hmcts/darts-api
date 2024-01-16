package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.audio.model.TransformedMediaDetailsDto;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransformedMediaRepository extends JpaRepository<TransformedMediaEntity, Integer> {

    @Query("""
        SELECT tm FROM TransformedMediaEntity tm
        WHERE tm.mediaRequest.id = :mediaRequestId
        """)
    List<TransformedMediaEntity> findByMediaRequestId(Integer mediaRequestId);


    @Query("""
        SELECT tm FROM TransformedMediaEntity tm
        WHERE tm.mediaRequest.id IN :ids
        """)
    List<TransformedMediaEntity> findByIdIn(List<Integer> ids);

    @Query("""
        SELECT new uk.gov.hmcts.darts.audio.model.TransformedMediaDetailsDto (mr.id,
        tm.id,
        case.id,
        he.id,
        mr.requestType,
        case.caseNumber,
        ch.courthouseName,
        he.hearingDate,
        tm.startTime,
        tm.endTime,
        tm.expiryTime,
        mr.status,
        tm.outputFilename,
        tm.outputFormat,
        tm.lastAccessed)

        FROM TransformedMediaEntity tm, MediaRequestEntity mr, HearingEntity he, CourtCaseEntity case, CourthouseEntity ch
        WHERE tm.mediaRequest = mr
        and mr.hearing = he
        and he.courtCase = case
        and case.courthouse = ch
        and mr.currentOwner.id = :userId
        and ((true = :expired and tm.expiryTime < current_timestamp and mr.status in ('EXPIRED', 'COMPLETED')) or
        (false = :expired and (tm.expiryTime is null or tm.expiryTime >= current_timestamp) and mr.status = 'COMPLETED'))
        order by 1
        """)
    List<TransformedMediaDetailsDto> findTransformedMediaDetails(Integer userId, boolean expired);


    @Query("""
        SELECT tm FROM MediaRequestEntity mr, TransformedMediaEntity tm
        WHERE tm.mediaRequest = mr
        AND ((tm.lastAccessed < :createdAtOrLastAccessedDateTime AND mr.status = 'COMPLETED')
             OR (tm.createdDateTime < :createdAtOrLastAccessedDateTime AND mr.status <> 'PROCESSING' AND tm.lastAccessed IS NULL))
        """)
    List<TransformedMediaEntity> findAllDeletableTransformedMedia(OffsetDateTime createdAtOrLastAccessedDateTime);

}
