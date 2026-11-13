package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;

import java.util.List;

public interface CaseLinkImportResultRepository extends JpaRepository<CaseLinkImportResultEntity, Long> {

    long countByImportId(Long importId);

    long countByImportIdAndStatusId(Long importId, Integer statusId);

    List<CaseLinkImportResultEntity> findByImportIdAndStatusIdOrderByRowNumber(Long importId, Integer statusId);

    Page<CaseLinkImportResultEntity> findByImportIdAndStatusId(Long importId, Integer statusId, Pageable pageable);
}
