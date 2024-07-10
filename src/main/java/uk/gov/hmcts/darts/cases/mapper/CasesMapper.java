package uk.gov.hmcts.darts.cases.mapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ReportingRestriction;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingReportingRestrictionsEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods"})
@Slf4j
public class CasesMapper {

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;
    private final CaseRetentionRepository caseRetentionRepository;
    private final AuthorisationApi authorisationApi;
    private final LogApi logApi;
    @Value("${darts.log.unallocated-case-regex}")
    private final Pattern unallocatedCaseRegex;

    public List<ScheduledCase> mapToScheduledCases(List<HearingEntity> hearings) {
        return emptyIfNull(hearings).stream().map(this::mapToScheduledCase)
            .sorted(comparing(ScheduledCase::getScheduledStart))
            .toList();
    }

    public PostCaseResponse mapToPostCaseResponse(CourtCaseEntity caseEntity) {
        PostCaseResponse postCaseResponse = new PostCaseResponse();
        postCaseResponse.setCourthouse(caseEntity.getCourthouse().getCourthouseName());
        postCaseResponse.setCaseNumber(caseEntity.getCaseNumber());
        postCaseResponse.setCaseId(caseEntity.getId());
        postCaseResponse.setDefendants(caseEntity.getDefendantStringList());
        postCaseResponse.setProsecutors(caseEntity.getProsecutorsStringList());
        postCaseResponse.setDefenders(caseEntity.getDefenceStringList());
        postCaseResponse.setJudges(caseEntity.getJudgeStringList());
        return postCaseResponse;
    }

    public ScheduledCase mapToScheduledCase(CourtCaseEntity caseEntity) {
        ScheduledCase scheduledCase = new ScheduledCase();
        scheduledCase.setCourthouse(caseEntity.getCourthouse().getCourthouseName());
        scheduledCase.setCaseNumber(caseEntity.getCaseNumber());
        scheduledCase.setDefendants(caseEntity.getDefendantStringList());
        scheduledCase.setProsecutors(caseEntity.getProsecutorsStringList());
        scheduledCase.setDefenders(caseEntity.getDefenceStringList());
        scheduledCase.setJudges(caseEntity.getJudgeStringList());
        return scheduledCase;
    }

    public ScheduledCase mapToScheduledCase(HearingEntity hearing) {
        CourtCaseEntity courtCase = hearing.getCourtCase();
        ScheduledCase scheduledCase = mapToScheduledCase(courtCase);
        scheduledCase.setHearingDate(hearing.getHearingDate());
        scheduledCase.setScheduledStart(toStringOrDefaultTo(hearing.getScheduledStartTime()));
        scheduledCase.setCourtroom(hearing.getCourtroom().getName());
        return scheduledCase;
    }

    public CourtCaseEntity addDefendantProsecutorDefenderJudge(CourtCaseEntity caseEntity, AddCaseRequest caseRequest) {

        emptyIfNull(caseRequest.getDefendants()).forEach(newDefendant -> {
            if (unallocatedCaseRegex.matcher(newDefendant).matches()) {
                logApi.defendantNotAdded(newDefendant, caseEntity.getCaseNumber());
            } else {
                caseEntity.addDefendant(createNewDefendant(newDefendant, caseEntity));
            }
        });

        emptyIfNull(caseRequest.getProsecutors()).forEach(newProsecutor -> {
            caseEntity.addProsecutor(createNewProsecutor(newProsecutor, caseEntity));
        });

        emptyIfNull(caseRequest.getDefenders()).forEach(newDefender -> {
            caseEntity.addDefence(createNewDefence(newDefender, caseEntity));
        });

        emptyIfNull(caseRequest.getJudges()).forEach(newJudge -> {
            caseEntity.addJudge(retrieveCoreObjectService.retrieveOrCreateJudge(newJudge));
        });

        return caseEntity;
    }

    @Transactional
    public SingleCase mapToSingleCase(CourtCaseEntity caseEntity) {
        SingleCase singleCase = new SingleCase();

        Optional<CaseRetentionEntity> caseRetentionOptional = caseRetentionRepository
            .findTopByCourtCaseAndCurrentStateOrderByCreatedDateTimeDesc(
                caseEntity,
                String.valueOf(CaseRetentionStatus.COMPLETE)
            );

        if (!caseRetentionOptional.isEmpty()) {
            CaseRetentionEntity caseRetention = caseRetentionOptional.get();
            RetentionPolicyTypeEntity retentionPolicy = caseRetention.getRetentionPolicyType();

            singleCase.setRetainUntilDateTime(caseRetention.getRetainUntil());
            singleCase.setRetentionDateTimeApplied(caseRetention.getRetainUntilAppliedOn());
            singleCase.setRetentionPolicyApplied(retentionPolicy.getPolicyName());

        }

        singleCase.setCaseClosedDateTime(caseEntity.getCaseClosedTimestamp());
        singleCase.setCaseId(caseEntity.getId());
        singleCase.setCaseNumber(caseEntity.getCaseNumber());
        singleCase.setCourthouseId(caseEntity.getCourthouse().getId());
        singleCase.setCourthouse(caseEntity.getCourthouse().getCourthouseName());
        singleCase.setDefendants(caseEntity.getDefendantStringList());
        singleCase.setDefenders(caseEntity.getDefenceStringList());
        singleCase.setProsecutors(caseEntity.getProsecutorsStringList());
        singleCase.setJudges(caseEntity.getJudgeStringList());

        var reportingRestrictions = hearingReportingRestrictionsRepository.findAllByCaseId(caseEntity.getId()).stream()
            .map(this::toReportingRestriction)
            .collect(toList());

        if (caseEntity.getReportingRestrictions() != null && reportingRestrictions.isEmpty()) {
            reportingRestrictions.add(
                reportingRestrictionWithName(caseEntity.getReportingRestrictions().getEventName()));
        }

        singleCase.setReportingRestrictions(sortedByTimestamp(reportingRestrictions));

        return singleCase;
    }

    private static List<ReportingRestriction> sortedByTimestamp(List<ReportingRestriction> reportingRestrictions) {
        return reportingRestrictions.stream()
            .sorted(comparing(ReportingRestriction::getEventTs))
            .collect(toList());
    }

    private static ReportingRestriction reportingRestrictionWithName(String name) {
        var reportingRestriction = new ReportingRestriction();
        reportingRestriction.setEventName(name);
        return reportingRestriction;
    }

    private ReportingRestriction toReportingRestriction(HearingReportingRestrictionsEntity restrictionsEntity) {
        var reportingRestriction = new ReportingRestriction();
        reportingRestriction.setEventId(restrictionsEntity.getEveId());
        reportingRestriction.setEventName(restrictionsEntity.getEventName());
        reportingRestriction.setEventText(restrictionsEntity.getEventText());
        reportingRestriction.setHearingId(restrictionsEntity.getHearingId());
        reportingRestriction.setEventTs(restrictionsEntity.getEventDateTime());
        return reportingRestriction;
    }

    private DefenceEntity createNewDefence(String newProsecutor, CourtCaseEntity caseEntity) {
        DefenceEntity defence = new DefenceEntity();
        defence.setCourtCase(caseEntity);
        defence.setName(newProsecutor);
        defence.setCreatedBy(authorisationApi.getCurrentUser());
        defence.setLastModifiedBy(authorisationApi.getCurrentUser());
        return defence;
    }

    private ProsecutorEntity createNewProsecutor(String newProsecutor, CourtCaseEntity caseEntity) {
        ProsecutorEntity prosecutor = new ProsecutorEntity();
        prosecutor.setCourtCase(caseEntity);
        prosecutor.setName(newProsecutor);
        prosecutor.setCreatedBy(authorisationApi.getCurrentUser());
        prosecutor.setLastModifiedBy(authorisationApi.getCurrentUser());
        return prosecutor;
    }

    private DefendantEntity createNewDefendant(String newDefendant, CourtCaseEntity caseEntity) {
        DefendantEntity defendant = new DefendantEntity();
        defendant.setCourtCase(caseEntity);
        defendant.setName(newDefendant);
        defendant.setCreatedBy(authorisationApi.getCurrentUser());
        defendant.setLastModifiedBy(authorisationApi.getCurrentUser());
        return defendant;
    }


    private String toStringOrDefaultTo(LocalTime time) {
        if (Objects.isNull(time)) {
            return "";
        }
        return time.truncatedTo(ChronoUnit.MINUTES).toString();
    }
}
