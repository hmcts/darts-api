package uk.gov.hmcts.darts.courthouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.util.Optional;

@Repository
public interface CourthouseRepository extends JpaRepository<CourthouseEntity, Integer> {

    Optional<CourthouseEntity> findByCode(int code);

    Optional<CourthouseEntity> findByCourthouseName(String name);
}
