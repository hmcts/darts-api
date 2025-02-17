package uk.gov.hmcts.darts.hearings.mapper;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.model.HearingsResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseCase;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseHearing;

public class AdminHearingMapper {
    public static HearingsResponse mapToHearingsResponse(HearingEntity hearingEntity) {
        if (hearingEntity == null) {
            return null;
        }
        HearingsResponse hearingsResponse = new HearingsResponse();
        hearingsResponse.setCase(mapToHearingsResponseCase(hearingEntity.getCourtCase()));
        hearingsResponse.setHearing(mapToHearingsResponseHearing(hearingEntity));
        return hearingsResponse;
    }

    public static HearingsResponseHearing mapToHearingsResponseHearing(HearingEntity hearingEntity) {
        if (hearingEntity == null) {
            return null;
        }
        HearingsResponseHearing hearingsResponseHearing = new HearingsResponseHearing();
        hearingsResponseHearing.setId(hearingEntity.getId());
        hearingsResponseHearing.setDate(hearingEntity.getHearingDate());
        hearingsResponseHearing.setScheduledStartTime(HearingCommonMapper.toTimeString(hearingEntity.getScheduledStartTime()));
        hearingsResponseHearing.setHearingTookPlace(hearingEntity.getHearingIsActual());
        hearingsResponseHearing.setAudit(HearingCommonMapper.mapToAudit(hearingEntity));
        hearingsResponseHearing.setJudges(HearingCommonMapper.asList(hearingEntity.getJudges(), HearingCommonMapper::mapToNameAndIdResponse));
        hearingsResponseHearing.setLocation(HearingCommonMapper.mapToLocation(hearingEntity.getCourtroom()));
        return hearingsResponseHearing;
    }


    public static HearingsResponseCase mapToHearingsResponseCase(CourtCaseEntity courtCase) {
        if (courtCase == null) {
            return null;
        }
        HearingsResponseCase hearingsResponseCase = new HearingsResponseCase();
        hearingsResponseCase.setId(courtCase.getId());
        hearingsResponseCase.setNumber(courtCase.getCaseNumber());
        hearingsResponseCase.setDefendants(HearingCommonMapper.asList(courtCase.getDefendantList(), HearingCommonMapper::mapToNameAndIdResponse));
        hearingsResponseCase.setProsecutors(HearingCommonMapper.asList(courtCase.getProsecutorList(), HearingCommonMapper::mapToNameAndIdResponse));
        hearingsResponseCase.setDefence(HearingCommonMapper.asList(courtCase.getDefenceList(), HearingCommonMapper::mapToNameAndIdResponse));
        return hearingsResponseCase;
    }
}
