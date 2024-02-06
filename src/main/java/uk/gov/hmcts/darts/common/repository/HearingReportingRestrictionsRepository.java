package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.HearingReportingRestrictionsEntity;

import java.util.List;

@Repository
public interface HearingReportingRestrictionsRepository extends JpaRepository<HearingReportingRestrictionsEntity, Integer> {

    List<HearingReportingRestrictionsEntity> findAllByCaseId(Integer caseId);
}
