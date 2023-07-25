package uk.gov.hmcts.darts.cases.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Component
@RequiredArgsConstructor
public class CasesMapper {

    private final CourthouseRepository courthouseRepository;

    public List<ScheduledCase> mapToCourtCases(List<HearingEntity> hearings) {
        return emptyIfNull(hearings).stream().map(this::mapToCourtCase)
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

    public CaseEntity mapAddCaseRequestToCaseEntity(AddCaseRequest addCaseRequest, CaseEntity caseEntity) {
        caseEntity.setCaseNumber(addCaseRequest.getCaseNumber());
        Optional.ofNullable(addCaseRequest.getDefendants()).ifPresent(l -> caseEntity.getDefendants().addAll(l));
        Optional.ofNullable(addCaseRequest.getProsecutors()).ifPresent(l -> caseEntity.getProsecutors().addAll(l));
        Optional.ofNullable(addCaseRequest.getDefenders()).ifPresent(l -> caseEntity.getDefenders().addAll(l));

        Optional<CourthouseEntity> foundEntity = courthouseRepository.findByCourthouseName(addCaseRequest.getCourthouse());
        foundEntity.ifPresentOrElse(caseEntity::setCourthouse, () -> {
            throw new DartsApiException(CaseApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST);
        });

        return caseEntity;
    }

    private String toStringOrDefaultTo(Object obj, String defaultStr) {
        if (Objects.isNull(obj)) {
            return defaultStr;
        }
        return obj.toString();
    }
}
