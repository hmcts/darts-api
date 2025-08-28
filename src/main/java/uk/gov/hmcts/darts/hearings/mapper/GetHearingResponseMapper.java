package uk.gov.hmcts.darts.hearings.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingReportingRestrictionsEntity;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.model.ReportingRestriction;

import java.util.Comparator;
import java.util.List;


@Component
@RequiredArgsConstructor
public class GetHearingResponseMapper {

    private final HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    @Transactional
    public GetHearingResponse map(HearingEntity hearing) {
        GetHearingResponse getHearingResponse = new GetHearingResponse();
        getHearingResponse.setHearingId(hearing.getId());
        getHearingResponse.setCourthouseId(hearing.getCourtroom().getCourthouse().getId());
        getHearingResponse.setCourthouse(hearing.getCourtroom().getCourthouse().getDisplayName());
        getHearingResponse.setCourtroom(hearing.getCourtroom().getName());
        getHearingResponse.setCaseId(hearing.getCourtCase().getId());
        getHearingResponse.setCaseNumber(hearing.getCourtCase().getCaseNumber());
        getHearingResponse.setHearingDate(hearing.getHearingDate());
        getHearingResponse.setJudges(hearing.getJudgesStringList());
        getHearingResponse.setTranscriptionCount(hearing.getTranscriptions().stream().filter(t -> BooleanUtils.isTrue(t.getIsCurrent())).toList().size());
        List<HearingReportingRestrictionsEntity> restrictions
            = hearingReportingRestrictionsRepository.findAllByCaseId(hearing.getCourtCase().getId());

        restrictions.sort(Comparator.comparing(HearingReportingRestrictionsEntity::getEventDateTime));
        restrictions.forEach(repRes -> getHearingResponse.addCaseReportingRestrictionsItem(buildReportingRestrictionFrom(repRes)));

        if (hearing.getCourtCase().getReportingRestrictions() != null && restrictions.isEmpty()) {
            getHearingResponse.addCaseReportingRestrictionsItem(
                reportingRestrictionWithName(hearing.getCourtCase().getReportingRestrictions().getEventName()));
        }

        return getHearingResponse;
    }

    private ReportingRestriction buildReportingRestrictionFrom(HearingReportingRestrictionsEntity restrictionsEntity) {
        var reportingRestriction = new ReportingRestriction();
        reportingRestriction.setEventId(restrictionsEntity.getEveId());
        reportingRestriction.setEventName(restrictionsEntity.getEventName());
        reportingRestriction.setEventText(restrictionsEntity.getEventText());
        reportingRestriction.setHearingId(restrictionsEntity.getHearingId());
        reportingRestriction.setEventTs(restrictionsEntity.getEventDateTime());
        return reportingRestriction;
    }

    public static ReportingRestriction reportingRestrictionWithName(String name) {
        var reportingRestriction = new ReportingRestriction();
        reportingRestriction.setEventName(name);
        return reportingRestriction;
    }

}