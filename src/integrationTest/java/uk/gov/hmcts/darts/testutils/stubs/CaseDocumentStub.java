package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
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
    private final UserAccountStub userAccountStub;

    @Transactional
    public CaseDocumentEntity createAndSaveCaseDocumentEntity(CourtCaseEntity courtCaseEntity, UserAccountEntity uploadedBy) {
        CaseDocumentEntity caseDocumentEntity = createCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocumentEntity = caseDocumentRepository.saveAndFlush(caseDocumentEntity);
        return caseDocumentEntity;
    }

    @Transactional
    public CaseDocumentEntity createAndSaveCaseDocumentEntity(CourtCaseEntity courtCaseEntity) {
        UserAccountEntity testUser = userAccountStub.getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocumentEntity = createCaseDocumentEntity(courtCaseEntity, testUser);
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
        caseDocumentEntity.setCreatedDateTime(OffsetDateTime.now(UTC));
        caseDocumentEntity.setLastModifiedBy(uploadedBy);
        return caseDocumentEntity;
    }

    public CaseDocumentEntity createCaseDocumentWithRandomValues() {
        //TODO move to abstract method
        EasyRandomParameters parameters = new EasyRandomParameters()
            .randomize(Integer.class, new IntegerRangeRandomizer(1, 100))
            .collectionSizeRange(1, 1)
            .overrideDefaultInitialization(true);

        EasyRandom generator = new EasyRandom(parameters);
        return generator.nextObject(CaseDocumentEntity.class);
    }
}
