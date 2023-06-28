package uk.gov.hmcts.darts.cases.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ReportingRestrictionsEntity;

@Repository
public interface ReportingRestrictionsRepository extends JpaRepository<ReportingRestrictionsEntity, Integer> {

}
