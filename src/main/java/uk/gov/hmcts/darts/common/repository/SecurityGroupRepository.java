package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SecurityGroupRepository extends RevisionRepository<SecurityGroupEntity, Integer, Long>, JpaRepository<SecurityGroupEntity, Integer> {

    Optional<SecurityGroupEntity> findByGroupNameIgnoreCase(String name);

    Optional<SecurityGroupEntity> findByDisplayNameIgnoreCase(String name);

    Optional<SecurityGroupEntity> findByGroupNameIgnoreCaseAndIdNot(String name, Integer id);

    Optional<SecurityGroupEntity> findByDisplayNameIgnoreCaseAndIdNot(String name, Integer id);

    boolean existsAllByIdIn(Set<Integer> ids);

    List<SecurityGroupEntity> findByIdGreaterThanEqual(Integer value);
}
