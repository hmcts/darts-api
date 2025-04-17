package uk.gov.hmcts.darts.testutils.stubs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

import static java.time.ZoneOffset.UTC;

@Component
@RequiredArgsConstructor
@Getter
@Deprecated
public class CaseDocumentStub {

    private final CaseDocumentRepository caseDocumentRepository;
    private final UserAccountStub userAccountStub;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;

    @Transactional
    public CaseDocumentEntity createAndSaveCaseDocumentEntity(CourtCaseEntity courtCaseEntity, UserAccountEntity uploadedBy) {
        CaseDocumentEntity caseDocumentEntity = createCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocumentEntity = dartsDatabaseSaveStub.save(caseDocumentEntity);
        return caseDocumentEntity;
    }

    @Transactional
    public CaseDocumentEntity createAndSaveCaseDocumentEntity(CourtCaseEntity courtCaseEntity) {
        UserAccountEntity testUser = userAccountStub.getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocumentEntity = createCaseDocumentEntity(courtCaseEntity, testUser);
        caseDocumentEntity = dartsDatabaseSaveStub.save(caseDocumentEntity);
        return caseDocumentEntity;
    }

    @Transactional
    public CaseDocumentEntity createAndSaveCaseDocumentEntity(CourtCaseEntity courtCaseEntity,
                                                              Consumer<CaseDocumentEntity> preSaveConsumer) {
        UserAccountEntity testUser = userAccountStub.getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocumentEntity = createCaseDocumentEntity(courtCaseEntity, testUser);
        preSaveConsumer.accept(caseDocumentEntity);
        caseDocumentEntity = dartsDatabaseSaveStub.save(caseDocumentEntity);
        return caseDocumentEntity;
    }

    public CaseDocumentEntity createCaseDocumentEntity(CourtCaseEntity courtCaseEntity, UserAccountEntity uploadedBy) {
        CaseDocumentEntity caseDocumentEntity = new CaseDocumentEntity();
        caseDocumentEntity.setCourtCase(courtCaseEntity);
        caseDocumentEntity.setFileName("test_filename");
        caseDocumentEntity.setFileType("docx");
        caseDocumentEntity.setFileSize(1234);
        caseDocumentEntity.setChecksum("xC3CCA7021CF79B42F245AF350601C284");
        caseDocumentEntity.setHidden(false);
        caseDocumentEntity.setCreatedBy(uploadedBy);
        caseDocumentEntity.setCreatedDateTime(OffsetDateTime.now(UTC));
        caseDocumentEntity.setLastModifiedBy(uploadedBy);
        return caseDocumentEntity;
    }
}