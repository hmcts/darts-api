package uk.gov.hmcts.darts.common.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.audio.model.TransformedMediaDetailsDto;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

@Repository
public interface TransformedMediaRepository extends JpaRepository<TransformedMediaEntity, Integer> {

    @Query("""
        SELECT tm FROM TransformedMediaEntity tm
        WHERE tm.mediaRequest.id = :mediaRequestId
        """)
    List<TransformedMediaEntity> findByMediaRequestId(Integer mediaRequestId);

    @Query("""
        SELECT new uk.gov.hmcts.darts.audio.model.TransformedMediaDetailsDto (mr.id,
        tm.id,
        case.id,
        he.id,
        mr.requestType,
        case.caseNumber,
        ch.displayName,
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
        and mr.currentOwner.active = true
        and ((true = :expired and tm.expiryTime < current_timestamp and mr.status in ('EXPIRED', 'COMPLETED')) or
        (false = :expired and (tm.expiryTime is null or tm.expiryTime >= current_timestamp) and mr.status = 'COMPLETED'))
        and mr.hearing = he
        order by tm.id DESC
        """)
    List<TransformedMediaDetailsDto> findTransformedMediaDetails(Integer userId, boolean expired);


    @Query("""
        SELECT tm.id FROM TransformedMediaEntity tm 
        JOIN tm.transientObjectDirectoryEntities tod        
        WHERE (
                (tm.lastAccessed < :createdAtOrLastAccessedDateTime AND tm.mediaRequest.status = 'COMPLETED')
             OR (tm.createdDateTime < :createdAtOrLastAccessedDateTime AND  tm.mediaRequest.status <> 'PROCESSING' AND tm.lastAccessed IS NULL)
        )
        AND upper(tod.status.description) <> 'MARKED FOR DELETION'   
        AND not exists (SELECT sge from tm.mediaRequest.currentOwner.securityGroupEntities sge 
               where sge.securityRoleEntity.roleName in ('MEDIA_IN_PERPETUITY', 'SUPER_ADMIN', 'SUPER_USER')) 
        """)
    List<Integer> findAllDeletableTransformedMedia(OffsetDateTime createdAtOrLastAccessedDateTime, Limit limit);

    @Query(value = """
        SELECT tm
            FROM TransformedMediaEntity tm
            JOIN tm.mediaRequest media
            JOIN media.hearing hearing
            JOIN hearing.courtCase courtCase
            JOIN hearing.courtroom courtroom
            JOIN courtroom.courthouse courthouse
        WHERE
           (:mediaId IS NULL OR (media.id=:mediaId)) AND
           (:caseNumber IS NULL OR (courtCase.caseNumber=cast(:caseNumber as text))) AND
           (:courtHouseDisplayName IS NULL OR (courthouse.displayName
           ILIKE CONCAT('%', cast(:courtHouseDisplayName as text), '%'))) AND
           ((cast(:hearingDate as LocalDate)) IS NULL OR (cast(:hearingDate as LocalDate) IS NOT NULL AND hearing.hearingDate=:hearingDate )) AND
           (:owner IS NULL OR (media.currentOwner.userFullName ILIKE CONCAT('%', cast(:owner as text), '%'))) AND
           (:requestedBy IS NULL OR (tm.createdBy.userFullName ILIKE CONCAT('%', cast (:requestedBy as text), '%'))) AND
           ((cast(:requestedAtFrom as TIMESTAMP)) IS NULL OR media.createdDateTime >= :requestedAtFrom) AND
           ((cast(:requestedAtTo as TIMESTAMP)) IS NULL OR (media.createdDateTime <= :requestedAtTo))
           ORDER BY tm.id DESC
        """)
    List<TransformedMediaEntity> findTransformedMedia(Integer mediaId,
                                                      String caseNumber,
                                                      String courtHouseDisplayName,
                                                      LocalDate hearingDate,
                                                      String owner,
                                                      String requestedBy,
                                                      OffsetDateTime requestedAtFrom,
                                                      OffsetDateTime requestedAtTo);
}