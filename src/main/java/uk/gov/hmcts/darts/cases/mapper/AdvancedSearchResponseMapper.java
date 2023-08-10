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
            if (hearing.getCourtCase().getId().equals(advancedSearchResult.getCaseID())) {
                //If it exists, just add extra details to the existing record.
                advancedSearchResult.addHearingsItem(mapToAdvancedSearchResultHearing(hearing));
                return;
            }
        }
        //case not already in response, so add it.
        advancedSearchResults.add(maptToAdvancedSearchResult(hearing));
    }

    private AdvancedSearchResult maptToAdvancedSearchResult(HearingEntity hearing) {
        AdvancedSearchResult advancedSearchResult = new AdvancedSearchResult();
        CourtCaseEntity courtCase = hearing.getCourtCase();
        advancedSearchResult.setCaseID(courtCase.getId());
        advancedSearchResult.setCaseNumber(courtCase.getCaseNumber());
        advancedSearchResult.setCourthouse(courtCase.getCourthouse().getCourthouseName());
        advancedSearchResult.setDefendants(courtCase.getDefendantStringList());
        advancedSearchResult.setJudges(courtCase.getJudgeStringList());

        advancedSearchResult.addHearingsItem(mapToAdvancedSearchResultHearing(hearing));

        EventHandlerEntity reportingRestrictions = courtCase.getReportingRestrictions();
        if (reportingRestrictions != null) {
            advancedSearchResult.setReportingRestriction(reportingRestrictions.getEventName());
        }
        return advancedSearchResult;
    }

    private AdvancedSearchResultHearing mapToAdvancedSearchResultHearing(HearingEntity hearing) {
        AdvancedSearchResultHearing advancedSearchResultHearing = new AdvancedSearchResultHearing();
        advancedSearchResultHearing.setId(hearing.getId());
        advancedSearchResultHearing.setDate(hearing.getHearingDate());
        advancedSearchResultHearing.setCourtroom(hearing.getCourtroom().getName());
        advancedSearchResultHearing.setJudges(hearing.getJudgesStringList());
        return advancedSearchResultHearing;
    }

}
