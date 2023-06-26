package uk.gov.hmcts.darts.courthouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.Courtroom;

@Repository
public interface CourtroomRepository extends JpaRepository<Courtroom, Integer> {

}
