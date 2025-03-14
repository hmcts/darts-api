package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResultHearing;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class AdvancedSearchResponseMapper {

    public List<AdvancedSearchResult> mapResponse(List<HearingEntity> hearings) {
        List<AdvancedSearchResult> advancedSearchResults = new ArrayList<>();
        for (HearingEntity hearing : hearings) {
            addHearingToResultList(advancedSearchResults, hearing);
        }
        return advancedSearchResults;
    }

    private void addHearingToResultList(List<AdvancedSearchResult> advancedSearchResults, HearingEntity hearing) {
        //check to see if caseId record is already in the response and add the hearing to it.
        for (AdvancedSearchResult advancedSearchResult : advancedSearchResults) {
            if (hearing.getCourtCase().getId().equals(advancedSearchResult.getCaseId())) {
                //If it exists, just add extra details to the existing record.
                advancedSearchResult.addHearingsItem(mapToAdvancedSearchResultHearing(hearing));
                return;
            }
        }
        //case not already in response, so add it.
        advancedSearchResults.add(mapToAdvancedSearchResult(hearing));
    }

    public AdvancedSearchResult mapToAdvancedSearchResult(HearingEntity hearing) {
        AdvancedSearchResult advancedSearchResult = new AdvancedSearchResult();
        CourtCaseEntity courtCase = hearing.getCourtCase();
        advancedSearchResult.setCaseId(courtCase.getId());
        advancedSearchResult.setCaseNumber(courtCase.getCaseNumber());
        advancedSearchResult.setCourthouse(courtCase.getCourthouse().getDisplayName());
        advancedSearchResult.setDefendants(courtCase.getDefendantStringList());
        advancedSearchResult.setJudges(courtCase.getJudgeStringList());
        advancedSearchResult.setIsDataAnonymised(courtCase.isDataAnonymised());
        advancedSearchResult.setDataAnonymisedAt(courtCase.getDataAnonymisedTs());

        advancedSearchResult.addHearingsItem(mapToAdvancedSearchResultHearing(hearing));

        EventHandlerEntity reportingRestrictions = courtCase.getReportingRestrictions();
        if (reportingRestrictions != null) {
            advancedSearchResult.setReportingRestriction(reportingRestrictions.getEventName());
        }
        return advancedSearchResult;
    }

    public AdvancedSearchResult mapToAdvancedSearchResult(CourtCaseEntity courtCase) { //NOSONAR
        AdvancedSearchResult advancedSearchResult = new AdvancedSearchResult(); //NOSONAR
        advancedSearchResult.setCaseId(courtCase.getId()); //NOSONAR
        advancedSearchResult.setCaseNumber(courtCase.getCaseNumber()); //NOSONAR
        advancedSearchResult.setCourthouse(courtCase.getCourthouse().getDisplayName()); //NOSONAR
        advancedSearchResult.setDefendants(courtCase.getDefendantStringList()); //NOSONAR
        advancedSearchResult.setJudges(courtCase.getJudgeStringList()); //NOSONAR
        advancedSearchResult.setIsDataAnonymised(courtCase.isDataAnonymised()); //NOSONAR
        advancedSearchResult.setDataAnonymisedAt(courtCase.getDataAnonymisedTs()); //NOSONAR

        courtCase.getHearings().forEach(hearingEntity -> { //NOSONAR
            advancedSearchResult.addHearingsItem(AdvancedSearchResponseMapper.mapToAdvancedSearchResultHearing(hearingEntity)); //NOSONAR
        }); //NOSONAR

        EventHandlerEntity reportingRestrictions = courtCase.getReportingRestrictions(); //NOSONAR
        if (reportingRestrictions != null) { //NOSONAR
            advancedSearchResult.setReportingRestriction(reportingRestrictions.getEventName()); //NOSONAR
        } //NOSONAR
        return advancedSearchResult; //NOSONAR
    } //NOSONAR

    public AdvancedSearchResultHearing mapToAdvancedSearchResultHearing(HearingEntity hearing) {
        AdvancedSearchResultHearing advancedSearchResultHearing = new AdvancedSearchResultHearing();
        advancedSearchResultHearing.setId(hearing.getId());
        advancedSearchResultHearing.setDate(hearing.getHearingDate());
        advancedSearchResultHearing.setCourtroom(hearing.getCourtroom().getName());
        advancedSearchResultHearing.setJudges(hearing.getJudgesStringList());
        return advancedSearchResultHearing;
    }

}
