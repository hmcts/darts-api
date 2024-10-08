package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CaseOverflowEntity;

@SuppressWarnings("PMD.MethodNamingConventions")
@Repository
public interface CaseOverflowRepository extends JpaRepository<CaseOverflowEntity, Integer> {


}
