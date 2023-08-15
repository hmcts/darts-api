package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;

import java.util.Optional;

@Repository
public interface ProsecutorRepository extends JpaRepository<ProsecutorEntity, Integer> {
    Optional<ProsecutorEntity> findByNameIgnoreCase(String name);

    default ProsecutorEntity createProsecutor(String prosecutorName, CourtCaseEntity courtCase) {
        ProsecutorEntity prosecutor = new ProsecutorEntity();
        prosecutor.setName(prosecutorName);
        prosecutor.setCourtCase(courtCase);
        this.saveAndFlush(prosecutor);
        return prosecutor;
    }
}
