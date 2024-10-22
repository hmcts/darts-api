package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.DataAnonymisationEntity;

@Repository
public interface DataAnonymisationRepository extends JpaRepository<DataAnonymisationEntity, Integer> {
}
