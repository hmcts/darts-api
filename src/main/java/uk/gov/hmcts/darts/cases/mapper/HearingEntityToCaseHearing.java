package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class HearingEntityToCaseHearing {

    public List<Hearing> mapToHearingList(List<HearingEntity> hearingEntities) {

        List<Hearing> hearings = new ArrayList<>();

        if (!hearingEntities.isEmpty()) {

            for (HearingEntity entity : hearingEntities) {
                hearings.add(mapToHearing(entity));
            }

        }
        return hearings;
    }

    private Hearing mapToHearing(HearingEntity entity) {

        Hearing hearing = new Hearing();

        hearing.setId(entity.getId());
        hearing.setDate(entity.getHearingDate());
        hearing.setJudges(entity.getJudgesStringList());
        hearing.setCourtroom(entity.getCourtroom().getName());

        return hearing;
    }

}
