package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomAnnotationDocumentEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomCourtCaseEntity;

import java.util.function.Consumer;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createDefenceForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createProsecutorForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class CaseTestData  implements Persistable<CustomCourtCaseEntity.CustomTranscriptionEntityBuilderRetrieve>  {

    public  CourtCaseEntity createSomeMinimalCase() {
        return someMinimal().getBuilder().build();
    }

    public  CourtCaseEntity caseWithCaseNumber(String caseNumber) {
        var someMinimalCase = createSomeMinimalCase();
        someMinimalCase.setCaseNumber(caseNumber);
        return someMinimalCase;
    }

    public  CourtCaseEntity createCaseWith(String caseNumber, CourthouseEntity courthouseEntity) {
        var courtCaseEntity = createSomeMinimalCase();

        if (courthouseEntity == null) {
            courtCaseEntity.setCourthouse(someMinimalCourthouse());
        } else {
            courtCaseEntity.setCourthouse(courthouseEntity);
        }

        courtCaseEntity.setCaseNumber(caseNumber);
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);
        return courtCaseEntity;
    }

    @Deprecated
    // Not a minimal case. refactor
    public CourtCaseEntity someMinimalCase() {
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCaseNumber("case-1");
        courtCaseEntity.setCourthouse(someMinimalCourthouse());
        courtCaseEntity.addDefendant(createDefendantForCaseWithName(courtCaseEntity, "aDefendant"));
        courtCaseEntity.addDefence(createDefenceForCaseWithName(courtCaseEntity, "aDefence"));
        courtCaseEntity.addProsecutor(createProsecutorForCaseWithName(courtCaseEntity, "aProsecutor"));
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);
        courtCaseEntity.setCreatedBy(UserAccountTestData.minimalUserAccount());
        courtCaseEntity.setLastModifiedBy(UserAccountTestData.minimalUserAccount());
        return courtCaseEntity;
    }

    /**
     * Creates a CourtCaseEntity. Passes the created case to the client for further customisations
     */
    public CourtCaseEntity someMinimalCase(Consumer<CourtCaseEntity> createdCaseConsumer) {
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCaseNumber("case-1");
        courtCaseEntity.setCourthouse(someMinimalCourthouse());
        courtCaseEntity.addDefendant(createDefendantForCaseWithName(courtCaseEntity, "aDefendant"));
        courtCaseEntity.addDefence(createDefenceForCaseWithName(courtCaseEntity, "aDefence"));
        courtCaseEntity.addProsecutor(createProsecutorForCaseWithName(courtCaseEntity, "aProsecutor"));
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);
        createdCaseConsumer.accept(courtCaseEntity);
        return courtCaseEntity;
    }

    public CourtCaseEntity createCaseAt(CourthouseEntity courthouse) {
        var courtCase = someMinimalCase();
        courtCase.setCourthouse(courthouse);
        return courtCase;
    }

    public CourtCaseEntity createCaseAt(CourthouseEntity courthouse, String caseNumber) {
        var courtCase = someMinimalCase();
        courtCase.setCourthouse(courthouse);
        courtCase.setCaseNumber(caseNumber);
        return courtCase;
    }

    public CourtCaseEntity createCaseWithCaseNumber(String caseNumber) {
        var courtCase = someMinimalCase();
        courtCase.setCaseNumber(caseNumber);
        return courtCase;
    }

    @Override
    public CustomCourtCaseEntity.CustomTranscriptionEntityBuilderRetrieve someMinimal() {
        CustomCourtCaseEntity.CustomTranscriptionEntityBuilderRetrieve retrieve = new CustomCourtCaseEntity.CustomTranscriptionEntityBuilderRetrieve();
        var postfix = random(10, false, true);
        var userAccount = minimalUserAccount();
        retrieve.getBuilder().courthouse(someMinimalCourthouse())
            .caseNumber("case-1-" + postfix)
            .closed(false)
            .interpreterUsed(false)
            .createdBy(userAccount)
            .lastModifiedBy(userAccount);
        return retrieve;
    }

    @Override
    public CustomCourtCaseEntity.CustomTranscriptionEntityBuilderRetrieve someMaximal() {
        return someMinimal();
    }
}