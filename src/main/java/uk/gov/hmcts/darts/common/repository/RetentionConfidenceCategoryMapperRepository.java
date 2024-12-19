package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.RetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;

import java.util.Optional;

@Repository
public interface RetentionConfidenceCategoryMapperRepository extends JpaRepository<RetentionConfidenceCategoryMapperEntity, Integer> {

    Optional<RetentionConfidenceCategoryMapperEntity> findByConfidenceCategory(RetentionConfidenceCategoryEnum confidenceCategory);

}
