package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.util.Optional;

@Repository
public interface CommonCourthouseRepository extends JpaRepository<CourthouseEntity, Integer> {

    Optional<CourthouseEntity> findByCourthouseNameIgnoreCase(String name);
}
