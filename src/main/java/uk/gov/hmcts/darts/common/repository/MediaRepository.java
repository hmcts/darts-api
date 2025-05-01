package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.task.runner.SoftDeleteRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public interface MediaRepository extends JpaRepository<MediaEntity, Long>,
    SoftDeleteRepository<MediaEntity, Long> {

    //Warning - This deliberately does not filter on me.isCurrent. Admin may want to see the old media.
    @Query("""
           SELECT me
           FROM HearingEntity he
           JOIN he.medias me
           WHERE he.id = :hearingId
           ORDER BY me.start
        """)
    List<MediaEntity> findAllByHearingId(Integer hearingId);

    @Query("""
           SELECT me
           FROM HearingEntity he
           JOIN he.medias me
           WHERE he.id = :hearingId
           AND me.isCurrent = true
           AND (me.isHidden = false OR me.isHidden = :includeHidden)
           ORDER BY me.start
        """)
    List<MediaEntity> findAllCurrentMediaByHearingId(Integer hearingId, boolean includeHidden);


    @Query("""
           SELECT me
           FROM CourtCaseEntity ca
           JOIN ca.hearings he
           JOIN he.medias me
           WHERE ca.id = :caseId
           ORDER BY me.start
        """)
    List<MediaEntity> findAllByCaseId(Integer caseId);

    @Query("""
           SELECT me
           FROM HearingEntity he
           JOIN he.medias me
           WHERE he.id = :hearingId
           AND me.channel = :channel
           AND me.isHidden = false
           AND me.isCurrent = true
           ORDER BY me.start DESC, me.end DESC
        """)
    List<MediaEntity> findAllByHearingIdAndChannelAndIsCurrentTrue(Integer hearingId, Integer channel);


    @Query("""
           SELECT me
           FROM MediaEntity me
           WHERE
           me.courtroom = :courtroomEntity
           AND me.channel = :channel
           AND me.mediaFile = :mediaFile
           AND me.start = :startedDateTime
           AND me.end = :endDateTime
           ORDER BY me.start
        """)
    List<MediaEntity> findMediaByDetails(CourtroomEntity courtroomEntity, Integer channel,
                                         String mediaFile, OffsetDateTime startedDateTime,
                                         OffsetDateTime endDateTime);

    @Query("""
        SELECT me
            FROM MediaEntity me
            JOIN me.hearings hearing
        WHERE
            (:hearingIds IS NULL OR (:hearingIds IS NOT NULL AND hearing.id in (:hearingIds)))
            AND (cast(:endAt as TIMESTAMP) IS NULL OR (me.end <= :endAt))
            AND (cast(:startAt as TIMESTAMP) IS NULL OR (me.start >= :startAt))
        """)
    List<MediaEntity> findMediaByDetails(List<Integer> hearingIds, OffsetDateTime startAt,
                                         OffsetDateTime endAt);

    //native query to bypass @SQLRestriction
    @Query(value = "SELECT me.* FROM darts.media me WHERE me.med_id = :mediaId", nativeQuery = true)
    Optional<MediaEntity> findByIdIncludeDeleted(Long mediaId);

    @Query("""
           select me
             from MediaEntity me
             where me.start <= :maxStartTime and me.end >= :minEndTime
             and me.courtroom.id = :courtroomId
             and me.isCurrent = true
        """)
    List<MediaEntity> findAllByCurrentMediaTimeContains(Integer courtroomId, OffsetDateTime maxStartTime, OffsetDateTime minEndTime);

    @Query("""
            SELECT me
            FROM MediaEntity me
            JOIN MediaLinkedCaseEntity mlce ON me.id = mlce.media.id
            WHERE mlce.courtCase.id = :caseId
        """)
    List<MediaEntity> findAllLinkedByMediaLinkedCaseByCaseId(Integer caseId);

    boolean existsByIdAndIsHiddenFalse(Long mediaId);

    @Query("""
           SELECT COUNT(me)
           FROM MediaEntity me
           WHERE me.chronicleId = :chronicleId
        """)
    Integer getVersionCount(String chronicleId);

    List<MediaEntity> findAllByChronicleId(String chronicleId);


    @Query("""
        SELECT distinct media
        FROM MediaEntity media
        JOIN media.hearings hearing
        WHERE hearing.courtCase.id = :caseId
        """)
    List<MediaEntity> findByCaseIdWithMediaList(Integer caseId);
}