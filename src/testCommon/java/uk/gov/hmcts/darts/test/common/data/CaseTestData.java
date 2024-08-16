package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createDefenceForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createProsecutorForCaseWithName;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CaseTestData {

    public static CourtCaseEntity createSomeMinimalCase() {
        var postfix = random(10, false, true);
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCourthouse(someMinimalCourthouse());
        courtCaseEntity.setCaseNumber("case-1-" + postfix);
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);

        courtCaseEntity.setCreatedBy(UserAccountTestData.minimalUserAccount());
        courtCaseEntity.setLastModifiedBy(UserAccountTestData.minimalUserAccount());
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now());
        courtCaseEntity.setLastModifiedDateTime(OffsetDateTime.now());
        return courtCaseEntity;
    }

    public static CourtCaseEntity createSomeMinimalCase(String caseNumber) {
        return createSomeMinimalCase(caseNumber, null);
    }

    public static CourtCaseEntity createSomeMinimalCase(String caseNumber, CourthouseEntity courthouseEntity) {
        var courtCaseEntity = new CourtCaseEntity();

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

    // Not a minimal case. refactor
    public static CourtCaseEntity someMinimalCase() {
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCaseNumber("case-1");
        courtCaseEntity.setCourthouse(someMinimalCourthouse());
        courtCaseEntity.addDefendant(createDefendantForCaseWithName(courtCaseEntity, "aDefendant"));
        courtCaseEntity.addDefence(createDefenceForCaseWithName(courtCaseEntity, "aDefence"));
        courtCaseEntity.addProsecutor(createProsecutorForCaseWithName(courtCaseEntity, "aProsecutor"));
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);

//        var userAccount = UserAccountTestData.minimalUserAccount();
//        courtCaseEntity.setCreatedBy(userAccount);
//        courtCaseEntity.setLastModifiedBy(userAccount);

        var now = OffsetDateTime.now();
        courtCaseEntity.setCreatedDateTime(now);
        courtCaseEntity.setLastModifiedDateTime(now);

        return courtCaseEntity;
    }

    /**
     * Creates a CourtCaseEntity. Passes the created case to the client for further customisations
     */
    public static CourtCaseEntity someMinimalCase(Consumer<CourtCaseEntity> createdCaseConsumer) {
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

    public static CourtCaseEntity createCaseAt(CourthouseEntity courthouse) {
        var courtCase = someMinimalCase();
        courtCase.setCourthouse(courthouse);
        return courtCase;
    }

    public static CourtCaseEntity createCaseAt(CourthouseEntity courthouse, String caseNumber) {
        var courtCase = someMinimalCase();
        courtCase.setCourthouse(courthouse);
        courtCase.setCaseNumber(caseNumber);
        return courtCase;
    }

    public static CourtCaseEntity createCaseWithCaseNumber(String caseNumber) {
        var courtCase = someMinimalCase();
        courtCase.setCaseNumber(caseNumber);
        return courtCase;
    }

    public static CourtCaseEntity createCaseAtCourthouse(String caseNumber, CourthouseEntity courthouse) {
        var courtCase = createCaseWithCaseNumber(caseNumber);
        courtCase.setCourthouse(courthouse);
        return courtCase;
    }

}