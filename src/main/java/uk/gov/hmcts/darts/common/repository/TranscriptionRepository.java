package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;

@Repository
public interface TranscriptionRepository extends JpaRepository<TranscriptionEntity, Integer> {

}
