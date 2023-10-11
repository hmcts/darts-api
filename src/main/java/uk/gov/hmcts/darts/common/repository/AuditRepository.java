package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AuditEntity;

@Repository
public interface AuditRepository extends JpaRepository<AuditEntity, Integer>, JpaSpecificationExecutor<AuditEntity> {

}
