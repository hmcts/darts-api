package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.DefenceTestData.createDefenceForCaseWithName;
import static uk.gov.hmcts.darts.testutils.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.testutils.data.ProsecutorTestData.createProsecutorForCaseWithName;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class CaseTestData {

    public static CourtCaseEntity someMinimalCase() {
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCaseNumber("case-1");
        courtCaseEntity.setCourthouse(someMinimalCourthouse());
        courtCaseEntity.addDefendant(createDefendantForCaseWithName(courtCaseEntity, "aDefendant"));
        courtCaseEntity.addDefence(createDefenceForCaseWithName(courtCaseEntity, "aDefence"));
        courtCaseEntity.addProsecutor(createProsecutorForCaseWithName(courtCaseEntity, "aProsecutor"));
        courtCaseEntity.setClosed(false);
        return courtCaseEntity;
    }

    public static CourtCaseEntity createCaseAt(CourthouseEntity courthouse) {
        var courtCaseEntity = someMinimalCase();
        courtCaseEntity.setCourthouse(courthouse);
        return courtCaseEntity;
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
