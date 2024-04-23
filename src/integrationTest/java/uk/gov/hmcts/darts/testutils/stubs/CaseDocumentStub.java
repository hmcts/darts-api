package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;

import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;

@Component
@RequiredArgsConstructor
@Getter
public class CaseDocumentStub {

    private final CaseDocumentRepository caseDocumentRepository;

    @Transactional
    public CaseDocumentEntity createAndSaveCaseDocumentEntity(CourtCaseEntity courtCaseEntity, UserAccountEntity uploadedBy) {
        CaseDocumentEntity caseDocumentEntity = createCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocumentEntity = caseDocumentRepository.saveAndFlush(caseDocumentEntity);
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
        caseDocumentEntity.setCreatedTs(OffsetDateTime.now(UTC));
        return caseDocumentEntity;
    }
}
