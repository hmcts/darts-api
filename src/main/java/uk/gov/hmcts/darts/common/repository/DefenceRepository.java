package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;

@Repository
public interface DefenceRepository extends JpaRepository<DefenceEntity, Integer> {

    default DefenceEntity createDefence(String defenceName, CourtCaseEntity courtCase) {
        DefenceEntity defence = new DefenceEntity();
        defence.setName(defenceName);
        defence.setCourtCase(courtCase);
        this.saveAndFlush(defence);
        return defence;
    }
}
