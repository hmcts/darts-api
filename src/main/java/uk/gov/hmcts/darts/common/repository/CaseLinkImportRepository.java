package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportEntity;

public interface CaseLinkImportRepository extends JpaRepository<CaseLinkImportEntity, Long> {
}
