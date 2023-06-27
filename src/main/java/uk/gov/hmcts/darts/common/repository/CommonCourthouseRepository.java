package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.Courthouse;

@Repository
public interface CommonCourthouseRepository extends JpaRepository<Courthouse, Integer> {
    Courthouse findByCourthouseNameIgnoreCase(String name);
}
