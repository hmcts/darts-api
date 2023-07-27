package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.someMinimalHearing;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class JudgeTestData {

    public static JudgeEntity someMinimalJudge() {
        var judgeEntity = new JudgeEntity();
        judgeEntity.setHearing(someMinimalHearing());
        judgeEntity.setName("Your Honour");
        return judgeEntity;
    }

    public static List<JudgeEntity> createListOfJudgesForHearing(int quantity, HearingEntity hearing) {
        return rangeClosed(1, quantity)
            .mapToObj(index -> createJudgeWithNameForHearing("Judge" + index, hearing))
            .collect(Collectors.toList());
    }


    public static JudgeEntity createJudgeWithNameForHearing(String name, HearingEntity hearing) {
        var judgeEntity = new JudgeEntity();
        judgeEntity.setName(name);
        judgeEntity.setHearing(hearing);
        return judgeEntity;
    }
}
