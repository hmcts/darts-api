package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class DefendantTestData {

    public static DefendantEntity someMinimalDefendant() {
        var defendant = new DefendantEntity();
        defendant.setCourtCase(someMinimalCase());
        defendant.setName("some-defendant");
        return defendant;
    }

    public static List<DefendantEntity> createListOfDefendantsForCase(int quantity, CourtCaseEntity courtCase) {
        return rangeClosed(1, quantity)
            .mapToObj(index -> {
                var defendant = createDefendantWithCaseBasedName(index, courtCase);
                defendant.setCourtCase(courtCase);
                return defendant;
            }).collect(Collectors.toList());
    }

    public static DefendantEntity createDefendantWithCaseBasedName(int index, CourtCaseEntity courtCase) {
        var defendant = someMinimalDefendant();
        defendant.setName("defendant_" + courtCase.getCaseNumber() + "_" + index);
        return defendant;
    }

    public static DefendantEntity createDefendantForCaseWithName(CourtCaseEntity courtCase, String name) {
        var defendant = new DefendantEntity();
        defendant.setCourtCase(courtCase);
        defendant.setName(name);
        return defendant;
    }

}
