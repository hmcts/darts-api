package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CaseRetentionRepositoryIntTest extends IntegrationBase {

    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_ROOM = "some-room";
    private static final String SOME_CASE_NUMBER_1 = "CASE1";
    private static final String SOME_CASE_NUMBER_2 = "CASE2";

    private final OffsetDateTime testTime = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private CaseRetentionRepository caseRetentionRepository;

    @Test
    void findByCaseId_returnsCaseRetention() {
        // Given
        transactionalUtil.executeInTransaction(() -> {

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

            // When
            var result = caseRetentionRepository.findByCaseId(courtCaseEntityWithCaseDocument.getId());

            // Then
            assertThat(result).hasSize(1);
            //assertThat(result.getFirst().getId()).isEqualTo(caseRetention1.getId());
        });
    }

}
