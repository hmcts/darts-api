package uk.gov.hmcts.darts.noderegistration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;

@Repository
public interface NodeRegistrationRepository extends JpaRepository<NodeRegisterEntity, Integer> {


}
