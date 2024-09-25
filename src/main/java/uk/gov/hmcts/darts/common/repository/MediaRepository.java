package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.task.runner.SoftDeleteRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Integer>,
    SoftDeleteRepository<MediaEntity, Integer> {

    //Warning - This deliberately does not filter on me.isCurrent. Admin may want to see the old media.
    @Query("""
           SELECT me
           FROM HearingEntity he
           JOIN he.mediaList me
           WHERE he.id = :hearingId
           ORDER BY me.start
        """)
    List<MediaEntity> findAllByHearingId(Integer hearingId);

    @Query("""
           SELECT me
           FROM HearingEntity he
           JOIN he.mediaList me
           WHERE he.id = :hearingId
           AND me.isCurrent = true
           ORDER BY me.start
        """)
    List<MediaEntity> findAllCurrentMediaByHearingId(Integer hearingId);

    @Query("""
           SELECT me
           FROM CourtCaseEntity ca
           JOIN ca.hearings he
           JOIN he.mediaList me
           WHERE ca.id = :caseId
           ORDER BY me.start
        """)
    List<MediaEntity> findAllByCaseId(Integer caseId);

    @Query("""
           SELECT me
           FROM HearingEntity he
           JOIN he.mediaList me
           WHERE he.id = :hearingId
           AND me.channel = :channel
           AND me.isHidden = false
           AND me.isCurrent = true
           ORDER BY me.start
        """)
    List<MediaEntity> findAllByHearingIdAndChannelAndIsCurrentTrue(Integer hearingId, Integer channel);


    @Query("""
           SELECT me
           FROM MediaEntity me
           WHERE
           me.courtroom = :courtroomEntity
           AND me.channel= :channel
           AND me.mediaFile= :mediaFile
           AND me.start= :startedDateTime
           AND me.end= :endDateTime
           ORDER BY me.start
        """)
    List<MediaEntity> findMediaByDetails(CourtroomEntity courtroomEntity, Integer channel,
                                         String mediaFile, OffsetDateTime startedDateTime,
                                         OffsetDateTime endDateTime);

    @Query(value = """
        SELECT me
            FROM MediaEntity me
            JOIN me.hearingList hearing
        WHERE
            (:hearingIds IS NULL OR (:hearingIds IS NOT NULL AND hearing.id in (:hearingIds)))
            AND (cast(:endAt as TIMESTAMP) IS NULL OR (me.end <= :endAt))
            AND (cast(:startAt as TIMESTAMP) IS NULL OR (me.start >= :startAt))
        """)
    List<MediaEntity> findMediaByDetails(List<Integer> hearingIds, OffsetDateTime startAt,
                                         OffsetDateTime endAt);

}