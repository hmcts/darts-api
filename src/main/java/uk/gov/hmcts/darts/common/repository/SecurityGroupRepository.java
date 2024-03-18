package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SecurityGroupRepository extends JpaRepository<SecurityGroupEntity, Integer> {

    Optional<SecurityGroupEntity> findByGroupNameIgnoreCase(String name);

    boolean existsAllByIdIn(Set<Integer> ids);

}
