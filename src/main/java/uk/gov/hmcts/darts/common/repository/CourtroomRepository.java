package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.Courtroom;

@Repository
public interface CourtroomRepository extends JpaRepository<Courtroom, Integer> {

    @Query("SELECT cr FROM Courthouse ch, Courtroom cr " +
        "WHERE upper(ch.courthouseName) = upper(:courthouse) " +
        "AND upper(cr.name) = upper(:courtroom) " +
        "AND cr.courthouse = ch.id "
    )
    Courtroom findByNames(String courthouse, String courtroom);


}
