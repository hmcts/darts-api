package uk.gov.hmcts.darts.annotation.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AnnotationStub;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationDocumentRepositoryIntTest extends IntegrationBase {

    @Autowired
    AnnotationRepository annotationRepository;
    @Autowired
    AnnotationStub annotationStub;
    @Autowired
    CourtCaseStub caseStub;

    @Autowired
    AnnotationDocumentRepository annotationDocumentRepository;

    @Test
    void testFindAllByCaseId() {

        // given
        var caseA = caseStub.createAndSaveCourtCaseWithHearings();
        var caseB = caseStub.createAndSaveCourtCaseWithHearings();

        var hear1A = caseA.getHearings().get(0);
        var hear2A = caseA.getHearings().get(1);
        var hear1B = caseB.getHearings().get(0);

        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var annotation1A = annotationStub.createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hear1A);
        var annotation2A = annotationStub.createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hear1A);
        var annotation3A = annotationStub.createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hear2A);
        var annotation1B = annotationStub.createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hear1B);

        annotation1A.addHearing(hear1B);
        annotationRepository.save(annotation1A);
        annotation2A.addHearing(hear2A);
        annotationRepository.save(annotation2A);

        var annotationDocument1 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation1A);
        var annotationDocument2 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation1A);
        var annotationDocument3 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation2A);
        var annotationDocument4 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation3A);
        var annotationDocument5 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation3A);
        annotationStub.createAndSaveAnnotationDocumentEntity(annotation1B);

        // when
        var result = annotationDocumentRepository.findAllByCaseId(caseA.getId());

        // then
        assertThat(result.stream().map(AnnotationDocumentEntity::getId))
            .containsExactlyInAnyOrder(
                annotationDocument1.getId(),
                annotationDocument2.getId(),
                annotationDocument3.getId(),
                annotationDocument4.getId(),
                annotationDocument5.getId());

        List<CourtCaseEntity> annotationDocument3Cases = annotationDocument3.associatedCourtCases();
        assertThat(annotationDocument3Cases.stream().map(CourtCaseEntity::getId)).containsExactlyInAnyOrder(caseA.getId());
        List<CourtCaseEntity> annotationDocument2Cases = annotationDocument2.associatedCourtCases();
        assertThat(annotationDocument2Cases.stream().map(CourtCaseEntity::getId)).containsExactlyInAnyOrder(caseA.getId(), caseB.getId());
    }
}
