package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class DefenceTestData {

    public static DefenceEntity someMinimalDefence() {
        var defence = new DefenceEntity();
        defence.setCourtCase(someMinimalCase());
        defence.setName("some-defence");
        return defence;
    }

    public static List<DefenceEntity> createListOfDefenceForCase(int quantity, CourtCaseEntity courtCase) {
        return rangeClosed(1, quantity)
              .mapToObj(index -> {
                  var defendant = createDefenceWithCaseBasedName(index, courtCase);
                  defendant.setCourtCase(courtCase);
                  return defendant;
              }).collect(Collectors.toList());
    }

    private static DefenceEntity createDefenceWithCaseBasedName(int index, CourtCaseEntity courtCase) {
        var defence = someMinimalDefence();
        defence.setName("defence_" + courtCase.getCaseNumber() + "_" + index);
        return defence;
    }

    public static DefenceEntity createDefenceForCaseWithName(CourtCaseEntity courtCase, String name) {
        var defence = new DefenceEntity();
        defence.setCourtCase(courtCase);
        defence.setName(name);
        return defence;
    }
}
