package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;

import java.util.Optional;

@Repository
public interface SecurityRoleRepository extends JpaRepository<SecurityRoleEntity, Integer> {

    Optional<SecurityRoleEntity> findByRoleName(String string);

}
