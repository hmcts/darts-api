package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;

class GenerateCaseDocumentProcessorIntTest extends IntegrationBase {

    private static final OffsetDateTime DT_2025 = OffsetDateTime.of(2025, 1, 1, 1, 0, 0, 0, UTC);

    @MockitoSpyBean
    CaseRepository caseRepository;
    @MockitoSpyBean
    CaseDocumentRepository caseDocumentRepository;
    @MockitoSpyBean
    ExternalObjectDirectoryRepository eodRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    AnnotationRepository annotationRepository;
    @Autowired
    GenerateCaseDocumentSingleCaseProcessor processor;
    @Autowired
    HearingRepository hearingRepository;

    @Test
    void testGenerateCaseDocument() {
        // given
        givenBearerTokenExists("darts.global.user@hmcts.net");

        CourtCaseEntity courtCase = dartsDatabase.getCourtCaseStub().createAndSaveCourtCaseWithHearings();

        courtCase.setRetentionUpdated(true);
        courtCase.setRetentionRetries(1);
        courtCase.setClosed(true);

        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        var hearing = courtCase.getHearings().getFirst();
        hearing.addMedia(medias.getFirst());

        dartsDatabase.save(hearing);
        dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(courtCase, DT_2025);

        dartsDatabase.getExternalObjectDirectoryStub().createAndSaveEod(medias.getFirst(), ARM_DROP_ZONE, ARM,
                                                                        eod -> {
                                                                        });

        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hearing);
        dartsDatabase.save(annotation);

        // when
        processor.processGenerateCaseDocument(courtCase.getId());
        // then
        transactionalUtil.executeInTransaction(() -> {
            CourtCaseEntity courtCaseToAssert = caseRepository.findById(courtCase.getId()).orElseThrow();
            List<CaseDocumentEntity> caseDocumentEntities = courtCaseToAssert.getCaseDocumentEntities();
            assertThat(caseDocumentEntities).hasSize(1);
            CaseDocumentEntity caseDocument = caseDocumentEntities.getFirst();
            assertThat(caseDocument.getCourtCase().getId()).isEqualTo(courtCaseToAssert.getId());

            List<ExternalObjectDirectoryEntity> eodCaseDocument = eodRepository.findByCaseDocument(caseDocument);
            assertThat(eodCaseDocument).hasSize(1);

            CourtCaseEntity courtCaseEntity = caseRepository.findById(caseDocument.getCourtCase().getId()).orElseThrow();
            assertThat(courtCaseEntity.isRetentionUpdated()).isTrue();
            assertThat(courtCaseEntity.getRetentionRetries()).isZero();
        });
    }

}
