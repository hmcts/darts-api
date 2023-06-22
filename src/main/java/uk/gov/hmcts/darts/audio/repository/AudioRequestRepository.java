package uk.gov.hmcts.darts.audio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.audio.entity.MediaRequest;

import java.util.List;

@Repository
public interface AudioRequestRepository extends JpaRepository<MediaRequest, Integer> {

    List<MediaRequest> findByRequestId(Integer requestId);
}
