package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportStatusEntity;

public interface CaseLinkImportStatusRepository extends JpaRepository<CaseLinkImportStatusEntity, Integer> {
}
