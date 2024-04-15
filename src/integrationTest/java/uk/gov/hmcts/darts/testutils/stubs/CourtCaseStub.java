package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.testutils.data.CaseTestData;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class CourtCaseStub {

    @Autowired
    CaseRepository caseRepository;

    public CourtCaseEntity createAndSaveMinimalCourtCase() {

        var courtCase = CaseTestData.createSomeMinimalCase();
        return caseRepository.save(courtCase);
    }

    /**
     * Creates a CourtCaseEntity. Passes the created case to the client for further customisations before saving
     */
    public CourtCaseEntity createAndSaveCourtCase(Consumer<CourtCaseEntity> caseConsumer) {

        var courtCase = createAndSaveMinimalCourtCase();
        caseConsumer.accept(courtCase);
        return caseRepository.save(courtCase);
    }
}
