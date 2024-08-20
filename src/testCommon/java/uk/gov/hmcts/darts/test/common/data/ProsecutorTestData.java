package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createSomeMinimalCase;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class ProsecutorTestData {

    public static ProsecutorEntity someMinimalProsecutor() {
        var prosecutor = new ProsecutorEntity();
        prosecutor.setCourtCase(createSomeMinimalCase());
        prosecutor.setName("some-prosecutor");
        var accountEntity = minimalUserAccount();
        prosecutor.setCreatedBy(accountEntity);
        prosecutor.setLastModifiedBy(accountEntity);
        return prosecutor;
    }

    public static List<ProsecutorEntity> createListOfProsecutor(int quantity, CourtCaseEntity courtCase) {
        return rangeClosed(1, quantity)
            .mapToObj(index -> {
                var prosecutor = createProsecutorWithCaseBasedName(index, courtCase);
                prosecutor.setCourtCase(courtCase);
                return prosecutor;
            }).collect(toList());
    }

    private static ProsecutorEntity createProsecutorWithCaseBasedName(int index, CourtCaseEntity courtCase) {
        var prosecutor = someMinimalProsecutor();
        prosecutor.setName("prosecutor_" + courtCase.getCaseNumber() + "_" + index);
        return prosecutor;
    }

    public static ProsecutorEntity createProsecutorForCaseWithName(CourtCaseEntity courtCase, String name) {
        var prosecutor = someMinimalProsecutorForCase(courtCase);
        prosecutor.setName(name);
        return prosecutor;
    }

    public static ProsecutorEntity someMinimalProsecutorForCase(CourtCaseEntity courtCase) {
        var prosecutor = someMinimalProsecutor();
        prosecutor.setCourtCase(courtCase);
        return prosecutor;
    }
}
