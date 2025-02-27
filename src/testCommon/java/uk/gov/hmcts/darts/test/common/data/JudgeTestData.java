package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class JudgeTestData {

    private JudgeTestData() {

    }

    public static JudgeEntity someMinimalJudge() {
        var judge = new JudgeEntity();
        judge.setName("some-judge");
        var userAccount = minimalUserAccount();
        judge.setCreatedBy(userAccount);
        judge.setLastModifiedBy(userAccount);
        return judge;
    }

    public static List<JudgeEntity> createListOfJudges(int quantity, CourtCaseEntity courtCase) {
        return rangeClosed(1, quantity)
            .mapToObj(index -> createJudgeWithName("Judge" + courtCase.getCaseNumber() + "_" + index))
            .collect(Collectors.toList());
    }


    public static JudgeEntity createJudgeWithName(String name) {
        var judgeEntity = someMinimalJudge();
        judgeEntity.setName(name.toUpperCase(Locale.ROOT));
        return judgeEntity;
    }
}