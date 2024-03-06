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
        WHERE upper(ch.courthouseName) = upper(:courthouse)
        AND upper(cr.name) = upper(:courtroom)
        AND h.hearingDate = :date
        AND h.courtroom = cr
        AND cr.courthouse = ch
        """
    )
    List<HearingEntity> findByCourthouseCourtroomAndDate(String courthouse, String courtroom, LocalDate date);

    @Query("""
        SELECT h FROM HearingEntity h, CourtCaseEntity case
        WHERE case.id in :caseIds
        AND h.courtCase = case
        """
    )
    List<HearingEntity> findByCaseIds(List<Integer> caseIds);

    @Query("""
        SELECT h FROM HearingEntity h, CourthouseEntity ch, CourtroomEntity cr, CourtCaseEntity case
        WHERE upper(ch.courthouseName) = upper(:courthouse)
        AND upper(cr.name) = upper(:courtroom)
        AND h.hearingDate = :date
        AND h.courtroom = cr
        AND cr.courthouse = ch
        and case.caseNumber = :caseNumber
        and h.courtCase = case
        """
    )
    Optional<HearingEntity> findHearing(String courthouse, String courtroom, String caseNumber, LocalDate date);

    @Query("""
        select exists
        (select he.id FROM HearingEntity he
        WHERE he.courtroom.id in (select courtroom.id from CourtroomEntity where courthouse.id = :courthouseId))
    """)
    boolean hearingsExistForCourthouse(Integer courthouseId);

    boolean existsById(Integer id);
}
