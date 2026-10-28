package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultStatusEntity;

public interface CaseLinkImportResultStatusRepository extends JpaRepository<CaseLinkImportResultStatusEntity, Integer> {
}
