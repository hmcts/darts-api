package uk.gov.hmcts.darts.hearings.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods"})
public class GetHearingResponseMapper {

    public GetHearingResponse map(HearingEntity hearing) {
        GetHearingResponse getHearingResponse = new GetHearingResponse();
        getHearingResponse.setHearingId(hearing.getId());
        getHearingResponse.setCourthouse(hearing.getCourtroom().getCourthouse().getCourthouseName());
        getHearingResponse.setCourtroom(hearing.getCourtroom().getName());
        getHearingResponse.setCaseId(hearing.getCourtCase().getId());
        getHearingResponse.setCaseNumber(hearing.getCourtCase().getCaseNumber());
        getHearingResponse.setHearingDate(hearing.getHearingDate());
        getHearingResponse.setJudges(hearing.getJudgesStringList());
        getHearingResponse.setTranscriptionCount(hearing.getTranscriptions().size());
        return getHearingResponse;
    }

}
