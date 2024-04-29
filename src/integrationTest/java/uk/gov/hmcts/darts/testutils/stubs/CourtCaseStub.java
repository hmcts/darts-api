package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.testutils.data.CaseTestData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class CourtCaseStub {

    private static final LocalDateTime D_2020_10_1 = LocalDateTime.of(2020, 10, 1, 12, 0, 0);
    private static final LocalDateTime D_2020_10_2 = LocalDateTime.of(2020, 10, 2, 12, 0, 0);

    @Autowired
    HearingStub hearingStub;

    @Autowired
    CaseRepository caseRepository;

    @Transactional
    public CourtCaseEntity createAndSaveMinimalCourtCase() {

        var courtCase = CaseTestData.createSomeMinimalCase();
        return caseRepository.save(courtCase);
    }

    /**
     * Creates a CourtCaseEntity. Passes the created case to the client for further customisations before saving
     */
    @Transactional
    public CourtCaseEntity createAndSaveCourtCase(Consumer<CourtCaseEntity> caseConsumer) {

        var courtCase = createAndSaveMinimalCourtCase();
        caseConsumer.accept(courtCase);
        return caseRepository.save(courtCase);
    }

    /**
     * Creates a CourtCaseEntity with 3 hearings. Passes the created case to the client for further customisations before saving
     */
    @Transactional
    public CourtCaseEntity createAndSaveCourtCaseWithHearings(Consumer<CourtCaseEntity> caseConsumer) {
        var courtCase = createAndSaveMinimalCourtCase();
        caseConsumer.accept(courtCase);

        var courthouseName = courtCase.getCourthouse().getCourthouseName();
        var hear1 = hearingStub.createHearing(courthouseName, "testCourtroom", courtCase.getCaseNumber(), D_2020_10_1);
        var hear2 = hearingStub.createHearing(courthouseName, "testCourtroom2", courtCase.getCaseNumber(), D_2020_10_1);
        var hear3 = hearingStub.createHearing(courthouseName, "testCourtroom", courtCase.getCaseNumber(), D_2020_10_2);
        courtCase.getHearings().addAll(List.of(hear1, hear2, hear3));

        return caseRepository.save(courtCase);
    }

    @Transactional
    public CourtCaseEntity createAndSaveCourtCaseWithHearings() {
        return createAndSaveCourtCaseWithHearings(courtCase -> { });
    }
}
