package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class DefendantTestData {

    private DefendantTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static DefendantEntity someMinimalDefendant() {
        var defendant = new DefendantEntity();
        defendant.setCourtCase(PersistableFactory.getCourtCaseTestData().createSomeMinimalCase());
        defendant.setName("some-defendant");
        var accountEntity = minimalUserAccount();
        defendant.setCreatedBy(accountEntity);
        defendant.setLastModifiedBy(accountEntity);
        return defendant;
    }

    public static DefendantEntity createDefendantForCase(CourtCaseEntity courtCase) {
        var defendant = someMinimalDefendant();
        defendant.setCourtCase(courtCase);
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
        var defendant = createDefendantForCase(courtCase);
        defendant.setName("defendant_" + courtCase.getCaseNumber() + "_" + index);
        return defendant;
    }

    public static DefendantEntity createDefendantForCaseWithName(CourtCaseEntity courtCase, String name) {
        var defendant = createDefendantForCase(courtCase);
        defendant.setName(name);
        return defendant;
    }

}