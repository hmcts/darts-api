package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@UtilityClass
public class GetCasesMapper {

    public List<ScheduledCase> mapToCourtCases(List<HearingEntity> hearings) {
        return emptyIfNull(hearings).stream().map(GetCasesMapper::mapToCourtCase)
            .sorted(Comparator.comparing(ScheduledCase::getScheduledStart))
            .toList();
    }

    public ScheduledCase mapToCourtCase(HearingEntity hearing) {
        CaseEntity hearingCourtCase = hearing.getCourtCase();

        ScheduledCase scheduledCase = new ScheduledCase();
        scheduledCase.setCourthouse(hearing.getCourtroom().getCourthouse().getCourthouseName());
        scheduledCase.setCourtroom(hearing.getCourtroom().getName());
        scheduledCase.setHearingDate(hearing.getHearingDate());
        scheduledCase.setCaseNumber(hearingCourtCase.getCaseNumber());
        scheduledCase.setScheduledStart(toStringOrDefaultTo(hearing.getScheduledStartTime(), ""));
        scheduledCase.setDefendants(hearingCourtCase.getDefendants());
        scheduledCase.setJudges(hearing.getJudges());
        scheduledCase.setProsecutors(hearingCourtCase.getProsecutors());
        scheduledCase.setDefenders(hearingCourtCase.getDefenders());
        return scheduledCase;
    }

    private String toStringOrDefaultTo(Object obj, String defaultStr) {
        if (Objects.isNull(obj)) {
            return defaultStr;
        }
        return obj.toString();
    }
}
