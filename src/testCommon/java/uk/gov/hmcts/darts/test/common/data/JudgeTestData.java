package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.getSystemUser;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class JudgeTestData {

    public static List<JudgeEntity> createListOfJudges(int quantity, CourtCaseEntity courtCase) {
        return rangeClosed(1, quantity)
            .mapToObj(index -> createJudgeWithName("Judge" + courtCase.getCaseNumber() + "_" + index))
            .collect(Collectors.toList());
    }


    public static JudgeEntity createJudgeWithName(String name) {
        var judgeEntity = new JudgeEntity();
        judgeEntity.setName(name.toUpperCase());
        judgeEntity.setCreatedBy(getSystemUser());
        judgeEntity.setLastModifiedBy(getSystemUser());
        return judgeEntity;
    }
}
