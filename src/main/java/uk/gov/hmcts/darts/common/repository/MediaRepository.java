package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Integer> {

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
           ORDER BY me.start
        """)
    List<MediaEntity> findAllByHearingIdAndChannel(Integer hearingId, Integer channel);


    @Query("""
           SELECT me
           FROM MediaEntity me
           JOIN me.courtroom cr
           JOIN cr.courthouse ch
           WHERE
           me.courtroom.id = cr.id
           AND cr.courthouse.id = ch.id
           AND cr.name= :courtroomName
           AND ch.courthouseName= :courthouseName
           AND me.channel= :channel
           AND me.mediaFile= :mediaFile
           AND me.start= :startedDateTime
           AND me.end= :endDateTime
           ORDER BY me.start
        """)
    List<MediaEntity> findMediaByDetails(String courthouseName,
                                   String courtroomName, Integer channel,
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