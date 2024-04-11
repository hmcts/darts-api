package uk.gov.hmcts.darts.annotation.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

public class AnnotationDocumentRepositoryIntTest extends IntegrationBase {

    @Autowired
    CourtCaseStub caseStub;

    @Autowired
    AnnotationDocumentRepository annotationDocumentRepository;

    @Test
    void testFindAllByCaseId() {
        var caseA = caseStub.createAndSaveMinimalCourtCase();

//        var hearA1 = hearingStub.createHearing(caseA.getCourthouse().getCourthouseName(), "testCourtroom", caseA.getCaseNumber(), D_2020_10_1);
//        var hearA2 = hearingStub.createHearing(caseA.getCourthouse().getCourthouseName(), "testCourtroom2", caseA.getCaseNumber(), D_2020_10_1);
//        var hearA3 = hearingStub.createHearing(caseA.getCourthouse().getCourthouseName(), "testCourtroom", caseA.getCaseNumber(), D_2020_10_2);
//        var hearB = hearingStub.createHearing(caseB.getCourthouse().getCourthouseName(), "testCourtroom", caseB.getCaseNumber(), D_2020_10_1);
//        caseA.setHearings(List.of(hearA1, hearA2, hearA3));
//        caseB.setHearings(List.of(hearB));
//        caseRepository.save(caseA);
//        caseRepository.save(caseB);

    }
}
