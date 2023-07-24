package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@UtilityClass
public class CasesMapper {

    public List<ScheduledCase> mapToCourtCases(List<HearingEntity> hearings) {
        return emptyIfNull(hearings).stream().map(CasesMapper::mapToCourtCase)
            .sorted(Comparator.comparing(ScheduledCase::getScheduledStart))
            .toList();
    }

    public ScheduledCase mapToCourtCase(CaseEntity caseEntity) {
        ScheduledCase scheduledCase = new ScheduledCase(caseEntity.getCourthouse().getCourthouseName());
        scheduledCase.setCaseNumber(caseEntity.getCaseNumber());
        scheduledCase.setDefendants(caseEntity.getDefendants());
        scheduledCase.setProsecutors(caseEntity.getProsecutors());
        scheduledCase.setDefenders(caseEntity.getDefenders());
        return scheduledCase;
    }

    public ScheduledCase mapToCourtCase(HearingEntity hearing, CaseEntity caseEntity) {
        CourtCaseEntity hearingCourtCase = hearing.getCourtCase();

        ScheduledCase scheduledCase = new ScheduledCase();
        if (hearing.getCourtroom() != null) {
            scheduledCase.setCourthouse(hearing.getCourtroom().getCourthouse().getCourthouseName());
            scheduledCase.setCourtroom(hearing.getCourtroom().getName());
        } else if (caseEntity != null) {
            scheduledCase.setCourthouse(caseEntity.getCourthouse().getCourthouseName());
        }
        scheduledCase.setHearingDate(hearing.getHearingDate());
        scheduledCase.setCaseNumber(hearingCourtCase.getCaseNumber());
        scheduledCase.setScheduledStart(toStringOrDefaultTo(hearing.getScheduledStartTime(), ""));
        scheduledCase.setDefendantList(hearingCourtCase.getDefendantStringList());
        scheduledCase.setJudgeList(hearing.getJudgesStringList());
        scheduledCase.setProsecutorList(hearingCourtCase.getProsecutorsStringList());
        scheduledCase.setDefenceList(hearingCourtCase.getDefenceStringList());
        return scheduledCase;
    }

    public ScheduledCase mapToCourtCase(HearingEntity hearing) {
        return mapToCourtCase(hearing, null);
    }

    private String toStringOrDefaultTo(Object obj, String defaultStr) {
        if (Objects.isNull(obj)) {
            return defaultStr;
        }
        return obj.toString();
    }
}
