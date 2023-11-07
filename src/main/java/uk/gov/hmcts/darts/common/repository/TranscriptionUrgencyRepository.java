package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;

import java.util.List;

@Repository
public interface TranscriptionUrgencyRepository extends JpaRepository<TranscriptionUrgencyEntity, Integer> {
    List<TranscriptionUrgencyEntity> findAllByDisplayStateTrue();

}
