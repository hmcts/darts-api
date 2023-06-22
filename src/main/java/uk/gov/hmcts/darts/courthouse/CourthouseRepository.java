package uk.gov.hmcts.darts.courthouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.Courthouse;

import java.util.Optional;

@Repository
public interface CourthouseRepository extends JpaRepository<Courthouse, Integer> {

    Optional<Courthouse> findByCode(int code);

    Optional<Courthouse> findByCourthouseName(String name);
}
