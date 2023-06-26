package uk.gov.hmcts.darts.courthouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.Courtroom;
import uk.gov.hmcts.darts.common.entity.ReportingRestrictions;

@Repository
public interface CourtroomRepository extends JpaRepository<Courtroom, Integer> {

}
