package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ReportingRestrictionsEntity;

import java.time.LocalDate;
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
        sortResponse(advancedSearchResults);
        return advancedSearchResults;
    }

    private void addHearingToResultList(List<AdvancedSearchResult> advancedSearchResults, HearingEntity hearing) {
        for (AdvancedSearchResult advancedSearchResult : advancedSearchResults) {
            if (hearing.getCourtCase().getId().equals(advancedSearchResult.getCaseID())) {
                //If it exists, just add extra details to the existing record.
                addUniqueJudge(advancedSearchResult, hearing.getJudges());
                addUniqueCourtroom(advancedSearchResult, hearing.getCourtroom().getName());
                addUniqueHearingDate(advancedSearchResult, hearing.getHearingDate());
                return;
            }
        }
        advancedSearchResults.add(maptToAdvancedSearchResult(hearing));
    }

    private void addUniqueJudge(AdvancedSearchResult advancedSearchResult, List<String> judges) {
        for (String judge : judges) {
            if (!advancedSearchResult.getJudges().contains(judge)) {
                advancedSearchResult.addJudgesItem(judge);
            }
        }
    }

    private void addUniqueCourtroom(AdvancedSearchResult advancedSearchResult, String courtroom) {
        if (!advancedSearchResult.getCourtrooms().contains(courtroom)) {
            advancedSearchResult.addCourtroomsItem(courtroom);
        }
    }

    private void addUniqueHearingDate(AdvancedSearchResult advancedSearchResult, LocalDate hearingDate) {
        if (!advancedSearchResult.getHearingDates().contains(hearingDate)) {
            advancedSearchResult.addHearingDatesItem(hearingDate);
        }
    }

    private AdvancedSearchResult maptToAdvancedSearchResult(HearingEntity hearing) {
        AdvancedSearchResult advancedSearchResult = new AdvancedSearchResult();
        advancedSearchResult.setCaseID(hearing.getCourtCase().getId());
        advancedSearchResult.setCaseNumber(hearing.getCourtCase().getCaseNumber());
        advancedSearchResult.setCourthouse(hearing.getCourtCase().getCourthouse().getCourthouseName());

        List<String> courtrooms = new ArrayList<>();
        courtrooms.add(hearing.getCourtroom().getName());
        advancedSearchResult.setCourtrooms(courtrooms);

        advancedSearchResult.setJudges(hearing.getJudges());
        advancedSearchResult.setDefendants(hearing.getCourtCase().getDefendants());

        List<LocalDate> hearingDates = new ArrayList<>();
        hearingDates.add(hearing.getHearingDate());
        advancedSearchResult.setHearingDates(hearingDates);

        ReportingRestrictionsEntity reportingRestrictions = hearing.getCourtCase().getReportingRestrictions();
        if (reportingRestrictions != null) {
            advancedSearchResult.setReportingRestriction(reportingRestrictions.getDescription());
        }
        return advancedSearchResult;
    }


    private void sortResponse(List<AdvancedSearchResult> advancedSearchResults) {
        for (AdvancedSearchResult advancedSearchResult : advancedSearchResults) {
            advancedSearchResult.setJudges(advancedSearchResult.getJudges().stream().sorted().toList());
            advancedSearchResult.setDefendants(advancedSearchResult.getDefendants().stream().sorted().toList());
            advancedSearchResult.setCourtrooms(advancedSearchResult.getCourtrooms().stream().sorted().toList());
            advancedSearchResult.setHearingDates(advancedSearchResult.getHearingDates().stream().sorted().toList());
        }
    }
}
