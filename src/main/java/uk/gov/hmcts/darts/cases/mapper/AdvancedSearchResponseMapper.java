package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResultHearing;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ReportingRestrictionsEntity;

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
        advancedSearchResult.setCaseID(hearing.getCourtCase().getId());
        advancedSearchResult.setCaseNumber(hearing.getCourtCase().getCaseNumber());
        advancedSearchResult.setCourthouse(hearing.getCourtCase().getCourthouse().getCourthouseName());
        advancedSearchResult.setDefendants(hearing.getCourtCase().getDefendantStringList());

        advancedSearchResult.addHearingsItem(mapToAdvancedSearchResultHearing(hearing));

        ReportingRestrictionsEntity reportingRestrictions = hearing.getCourtCase().getReportingRestrictions();
        if (reportingRestrictions != null) {
            advancedSearchResult.setReportingRestriction(reportingRestrictions.getDescription());
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
