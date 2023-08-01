package uk.gov.hmcts.darts.cases.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
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

    public ScheduledCase mapToCourtCase(CourtCaseEntity caseEntity) {
        ScheduledCase scheduledCase = new ScheduledCase(caseEntity.getCourthouse().getCourthouseName());
        scheduledCase.setCaseNumber(caseEntity.getCaseNumber());
        scheduledCase.setDefendantList(caseEntity.getDefendantStringList());
        scheduledCase.setProsecutorList(caseEntity.getProsecutorsStringList());
        scheduledCase.setDefenders(caseEntity.getDefenceStringList());
        return scheduledCase;
    }

    public ScheduledCase mapToCourtCase(HearingEntity hearing, CourtCaseEntity caseEntity) {
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
        scheduledCase.setDefenders(hearingCourtCase.getDefenceStringList());
        return scheduledCase;
    }

    public ScheduledCase mapToCourtCase(HearingEntity hearing) {
        return mapToCourtCase(hearing, null);
    }

    public CourtCaseEntity mapAddCaseRequestToCaseEntity(AddCaseRequest addCaseRequest, CourtCaseEntity caseEntity) {
        caseEntity.setCaseNumber(addCaseRequest.getCaseNumber());
        mapDefendantProsecutorDefender(caseEntity, addCaseRequest);

        Optional<CourthouseEntity> foundEntity = courthouseRepository.findByCourthouseName(addCaseRequest.getCourthouse());
        foundEntity.ifPresentOrElse(caseEntity::setCourthouse, () -> {
            throw new DartsApiException(CaseApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST);
        });

        return caseEntity;
    }

    private void mapDefendantProsecutorDefender(CourtCaseEntity caseEntity, AddCaseRequest caseRequest) {

        emptyIfNull(caseRequest.getDefendants()).forEach(newDefendant -> {
            Optional<DefendantEntity> found = emptyIfNull(caseEntity.getDefendantList())
                .stream().filter(d -> d.getName().equals(newDefendant)).findAny();
            found.ifPresentOrElse(
                d -> { }, () -> caseEntity.getDefendantList().add(createNewDefendant(newDefendant, caseEntity))
            );
        });

        emptyIfNull(caseRequest.getProsecutors()).forEach(newProsecutor -> {
            Optional<ProsecutorEntity> found = emptyIfNull(caseEntity.getProsecutorList())
                .stream().filter(d -> d.getName().equals(newProsecutor)).findAny();
            found.ifPresentOrElse(
                p -> { }, () -> caseEntity.getProsecutorList().add(createNewProsecutor(newProsecutor, caseEntity))
            );
        });

        emptyIfNull(caseRequest.getDefenders()).forEach(newDefender -> {
            Optional<DefenceEntity> found = emptyIfNull(caseEntity.getDefenceList())
                .stream().filter(d -> d.getName().equals(newDefender)).findAny();
            found.ifPresentOrElse(
                d -> { },
                () -> caseEntity.getDefenceList().add(createNewDefence(newDefender, caseEntity))
            );
        });
    }


    private DefenceEntity createNewDefence(String newProsecutor, CourtCaseEntity caseEntity) {
        DefenceEntity defence = new DefenceEntity();
        defence.setCourtCase(caseEntity);
        defence.setName(newProsecutor);
        return defence;
    }

    private ProsecutorEntity createNewProsecutor(String newProsecutor, CourtCaseEntity caseEntity) {
        ProsecutorEntity prosecutor = new ProsecutorEntity();
        prosecutor.setCourtCase(caseEntity);
        prosecutor.setName(newProsecutor);
        return prosecutor;
    }

    private DefendantEntity createNewDefendant(String newDefendant, CourtCaseEntity caseEntity) {
        DefendantEntity defendant = new DefendantEntity();
        defendant.setCourtCase(caseEntity);
        defendant.setName(newDefendant);
        return defendant;
    }


    private String toStringOrDefaultTo(Object obj, String defaultStr) {
        if (Objects.isNull(obj)) {
            return defaultStr;
        }
        return obj.toString();
    }
}
