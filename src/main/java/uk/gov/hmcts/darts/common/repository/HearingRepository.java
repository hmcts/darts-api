package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.Hearing;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HearingRepository extends JpaRepository<Hearing, Integer> {

    @Query("SELECT h FROM Hearing h, Courthouse ch, Courtroom cr " +
        "WHERE upper(ch.courthouseName) = upper(:courthouse) " +
        "AND upper(cr.name) = upper(:courtroom) " +
        "AND h.hearingDate = :date " +
        "AND h.courtroom = cr.id " +
        "AND cr.courthouse = ch.id "
    )
    List<Hearing> findByCourthouseCourtroomAndDate(String courthouse, String courtroom, LocalDate date);


}
