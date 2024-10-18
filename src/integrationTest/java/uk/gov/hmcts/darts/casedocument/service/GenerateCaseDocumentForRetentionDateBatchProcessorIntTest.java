package uk.gov.hmcts.darts.casedocument.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GenerateCaseDocumentForRetentionDateBatchProcessorIntTest extends IntegrationBase {
    protected static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    protected static final String SOME_ROOM = "some-room";
    protected static final String SOME_CASE_NUMBER_1 = "CASE1";
    protected static final String SOME_CASE_NUMBER_2 = "CASE2";

    @MockBean
    private UserIdentity mockUserIdentity;

    private final OffsetDateTime testTime = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private GenerateCaseDocumentForRetentionDateProcessor generateCaseDocumentForRetentionDateBatchProcessor;

    @BeforeEach
    void setupData() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void testProcessGenerateCaseDocumentForRetentionDateSuccess() {
        // given
        CourtCaseEntity courtCaseEntityWithNoCaseDocuments = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
        dartsDatabase.save(caseRetentionObject2);

        CourtCaseEntity courtCaseEntityWithCaseDocument = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(10), false);
        dartsDatabase.save(caseRetentionObject3);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocument, uploadedBy);
        caseDocument.setCreatedDateTime(OffsetDateTime.now().minusDays(30));
        dartsDatabase.save(caseDocument);

        // when
        generateCaseDocumentForRetentionDateBatchProcessor.processGenerateCaseDocumentForRetentionDate(2);

        // then
        CourtCaseEntity courtCaseEntity1 = dartsDatabase.getCaseRepository().findById(courtCaseEntityWithNoCaseDocuments.getId()).get();
        assertTrue(courtCaseEntity1.isRetentionUpdated());
        assertEquals(0, courtCaseEntity1.getRetentionRetries());

        List<CaseDocumentEntity> caseDocumentEntities1 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity1);
        assertEquals(1, caseDocumentEntities1.size());

        CourtCaseEntity courtCaseEntity2 = dartsDatabase.getCaseRepository().findById(courtCaseEntityWithCaseDocument.getId()).get();
        assertTrue(courtCaseEntity2.isRetentionUpdated());
        assertEquals(0, courtCaseEntity2.getRetentionRetries());

        List<CaseDocumentEntity> caseDocumentEntities2 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity2);
        assertEquals(2, caseDocumentEntities2.size());

    }

    @Test
    void testProcessGenerateCaseDocumentForRetentionDateRetentionDateTooFarInTheFutureNoDocumentGenerated() {
        // given
        CourtCaseEntity courtCaseEntityWithNoCaseDocuments = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CourtCaseEntity courtCaseEntityWithCaseDocument = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(40), false);
        dartsDatabase.save(caseRetentionObject3);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntityWithCaseDocument, uploadedBy);
        caseDocument.setCreatedDateTime(OffsetDateTime.now().minusDays(30));
        dartsDatabase.save(caseDocument);

        // when
        generateCaseDocumentForRetentionDateBatchProcessor.processGenerateCaseDocumentForRetentionDate(2);

        // then
        assertFalse(
            dartsDatabase.getCaseRepository()
                .findById(courtCaseEntityWithNoCaseDocuments.getId()).orElseThrow()
                .isRetentionUpdated());

        assertFalse(
            dartsDatabase.getCaseRepository()
                .findById(courtCaseEntityWithCaseDocument.getId()).orElseThrow()
                .isRetentionUpdated());
    }

    @Test
    void testProcessGenerateCaseDocumentForRetentionDateWithRecentDocumentsNoDocumentGenerated() {
        // given
        CourtCaseEntity courtCaseEntityWithCaseDocuments1 = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocuments1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocuments1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
        dartsDatabase.save(caseRetentionObject2);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument1 = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocuments1, uploadedBy);
        caseDocument1.setCreatedDateTime(OffsetDateTime.now().minusDays(27));
        dartsDatabase.save(caseDocument1);

        CourtCaseEntity courtCaseEntityWithCaseDocument2 = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument2, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(10), false);
        dartsDatabase.save(caseRetentionObject3);

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocument2, uploadedBy);
        dartsDatabase.save(caseDocument);

        // when
        generateCaseDocumentForRetentionDateBatchProcessor.processGenerateCaseDocumentForRetentionDate(2);

        // then
        assertFalse(
            dartsDatabase.getCaseRepository()
                .findById(courtCaseEntityWithCaseDocuments1.getId()).orElseThrow()
                .isRetentionUpdated());

        assertFalse(
            dartsDatabase.getCaseRepository()
                .findById(courtCaseEntityWithCaseDocument2.getId()).orElseThrow()
                .isRetentionUpdated());
    }

    @Test
    void testProcessGenerateCaseDocumentForRetentionDateWithIsRetentionUpdateTrueNoDocumentGenerated() {
        // given
        CourtCaseEntity courtCaseEntityWithNoCaseDocuments = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);
        courtCaseEntityWithNoCaseDocuments.setRetentionUpdated(true);
        dartsDatabase.save(courtCaseEntityWithNoCaseDocuments);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
        dartsDatabase.save(caseRetentionObject2);

        CourtCaseEntity courtCaseEntityWithCaseDocument = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);
        courtCaseEntityWithCaseDocument.setRetentionUpdated(true);
        dartsDatabase.save(courtCaseEntityWithCaseDocument);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(10), false);
        dartsDatabase.save(caseRetentionObject3);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocument, uploadedBy);
        caseDocument.setCreatedDateTime(OffsetDateTime.now().minusDays(30));
        dartsDatabase.save(caseDocument);

        // when
        generateCaseDocumentForRetentionDateBatchProcessor.processGenerateCaseDocumentForRetentionDate(2);

        // then
        CourtCaseEntity courtCaseEntity1 = dartsDatabase.getCaseRepository().getReferenceById(courtCaseEntityWithNoCaseDocuments.getId());
        List<CaseDocumentEntity> caseDocumentEntities1 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity1);
        assertEquals(0, caseDocumentEntities1.size());

        CourtCaseEntity courtCaseEntity2 = dartsDatabase.getCaseRepository().getReferenceById(courtCaseEntityWithCaseDocument.getId());
        List<CaseDocumentEntity> caseDocumentEntities2 =
            dartsDatabase.getCaseDocumentStub().getCaseDocumentRepository().findByCourtCase(courtCaseEntity2);
        assertEquals(1, caseDocumentEntities2.size());

    }
}
