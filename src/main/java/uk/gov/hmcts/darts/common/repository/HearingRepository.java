package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HearingRepository extends JpaRepository<HearingEntity, Integer> {

    @Query("""
        SELECT h FROM HearingEntity h, CourthouseEntity ch, CourtroomEntity cr
        WHERE ch.courthouseName = upper(trim(:courthouseName))
        AND cr.name = upper(trim(:courtroom))
        AND h.hearingDate = :date
        AND h.courtroom = cr
        AND cr.courthouse = ch
        """
    )
    List<HearingEntity> findByCourthouseCourtroomAndDate(String courthouseName, String courtroom, LocalDate date);

    @Query("""
        SELECT h FROM HearingEntity h
        WHERE h.courtCase.id in :caseIds
        """
    )
    List<HearingEntity> findByCaseIds(List<Integer> caseIds);

    @Query("""
        SELECT h FROM HearingEntity h
        WHERE h.courtCase.id in :caseIds
        AND h.hearingIsActual = true
        ORDER BY h.courtCase.caseNumber desc
        """
    )
    List<HearingEntity> findByIsActualCaseIds(List<Integer> caseIds);

    @Query("""
        SELECT h.id FROM HearingEntity h
        JOIN h.events event
        WHERE event.id = :eventId
        """
    )
    List<Integer> findHearingIdsByEventId(Integer eventId);

    @Query("""
        SELECT h FROM HearingEntity h
        JOIN h.mediaList media
        WHERE media.id = :mediaId
        """
    )
    List<HearingEntity> findHearingIdsByMediaId(Integer mediaId);

    @Query("""
        SELECT h FROM HearingEntity h, CourthouseEntity ch, CourtroomEntity cr, CourtCaseEntity case
        WHERE ch.courthouseName = upper(trim(:courthouseName))
        AND cr.name = upper(trim(:courtroom))
        AND h.hearingDate = :date
        AND h.courtroom = cr
        AND cr.courthouse = ch
        and case.caseNumber = :caseNumber
        and h.courtCase = case
        """
    )
    Optional<HearingEntity> findHearing(String courthouseName, String courtroom, String caseNumber, LocalDate date);

    @Query("""
            select exists
            (select he.id FROM HearingEntity he
            WHERE he.courtroom.id in (select courtroom.id from CourtroomEntity where courthouse.id = :courthouseId))
        """)
    boolean hearingsExistForCourthouse(Integer courthouseId);

    @Override
    boolean existsById(Integer id);

    @Query("""
        SELECT hearing
        FROM HearingEntity hearing
        WHERE (:caseNumber IS NULL OR hearing.courtCase.caseNumber ILIKE CONCAT('%', cast(:caseNumber as text), '%'))
            AND (:courthouseIds IS NULL OR hearing.courtroom.courthouse.id in (:courthouseIds))
            AND (:courtroomName IS NULL OR hearing.courtroom.name ILIKE CONCAT('%', cast(:courtroomName as text), '%'))
            AND (cast(:startDate as LocalDate) IS NULL OR hearing.hearingDate >= :startDate)
            AND (cast(:endDate as LocalDate) IS NULL OR hearing.hearingDate <= :endDate)
            ORDER BY hearing.hearingDate DESC
            LIMIT :numberOfRecords
        """)
    List<HearingEntity> findHearingDetails(List<Integer> courthouseIds, String caseNumber,
                                           String courtroomName,
                                           LocalDate startDate, LocalDate endDate, Integer numberOfRecords);

    @Query("""
        SELECT hearing
        FROM HearingEntity hearing, CourtCaseEntity case
        LEFT JOIN FETCH hearing.mediaList
        WHERE case.id = :caseId
        AND hearing.courtCase = case
        """)
    Optional<HearingEntity> findByCaseIdWithMediaList(Integer caseId);
}