package uk.gov.hmcts.darts.cases.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.Case;
import uk.gov.hmcts.darts.common.entity.Courthouse;

import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Integer> {

}
