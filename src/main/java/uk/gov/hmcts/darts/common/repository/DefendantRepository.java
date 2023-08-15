package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;

import java.util.Optional;

@Repository
public interface DefendantRepository extends JpaRepository<DefendantEntity, Integer> {
    Optional<DefendantEntity> findByNameIgnoreCase(String name);

    default DefendantEntity createDefendant(String defendantName, CourtCaseEntity courtCase) {
        DefendantEntity defendant = new DefendantEntity();
        defendant.setName(defendantName);
        defendant.setCourtCase(courtCase);
        this.saveAndFlush(defendant);
        return defendant;
    }
}
