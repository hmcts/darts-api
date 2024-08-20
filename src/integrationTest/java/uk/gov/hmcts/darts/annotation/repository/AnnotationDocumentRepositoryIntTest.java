package uk.gov.hmcts.darts.annotation.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.test.common.data.AnnotationDocumentTestData;
import uk.gov.hmcts.darts.test.common.data.HearingTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AnnotationStub;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createSomeMinimalCase;

class AnnotationDocumentRepositoryIntTest extends IntegrationBase {

    @Autowired
    AnnotationRepository annotationRepository;
    @Autowired
    AnnotationStub annotationStub;
    @Autowired
    CourtCaseStub caseStub;

    @Autowired
    AnnotationDocumentRepository annotationDocumentRepository;

    @BeforeEach
    void openHibernateSession() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void findsAnnotationDocumentsByCaseId() {
        // given
        var courtCaseA = createSomeMinimalCase();
        var courtCaseB = createSomeMinimalCase();
        var annotationDocument1 = createAnnotationDocumentForCases(courtCaseA);
        var annotationDocument2 = createAnnotationDocumentForCases(courtCaseA);
        var annotationDocument3 = createAnnotationDocumentForCases(courtCaseA);
        createAnnotationDocumentForCases(courtCaseB);
        createAnnotationDocumentForCases(courtCaseB);
        dartsPersistence.saveAll(annotationDocument1, annotationDocument2, annotationDocument3);

        // when
        var result = annotationDocumentRepository.findAllByCaseId(courtCaseA.getId());

        // then
        assertThat(result.stream().map(AnnotationDocumentEntity::getId))
            .containsExactlyInAnyOrder(
                annotationDocument1.getId(),
                annotationDocument2.getId(),
                annotationDocument3.getId());
    }

    @Test
    void canGetAssociatedCasesFromAnnotationDocument () {
        var courtCaseA = createSomeMinimalCase();
        var courtCaseB = createSomeMinimalCase();
        var annotationDocument1 = createAnnotationDocumentForCases(courtCaseA, courtCaseB);

        assertThat(annotationDocument1.associatedCourtCases()).containsExactlyInAnyOrder(courtCaseA, courtCaseB);
    }

    private AnnotationDocumentEntity createAnnotationDocumentForCases(CourtCaseEntity... courtCases) {
        var hearingEntities = stream(courtCases).map(HearingTestData::createHearingFor).toList();
        var annotationDocument = AnnotationDocumentTestData.createAnnotationDocumentForHearings(hearingEntities);
        return dartsPersistence.save(annotationDocument);
    }

}
