package uk.gov.hmcts.darts.hearings.service;

import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.model.Annotation;
import uk.gov.hmcts.darts.hearings.model.EventResponse;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.model.Transcript;

import java.util.List;

public interface HearingsService {

    GetHearingResponse getHearings(Integer hearingId);

    HearingEntity getHearingByIdWithValidation(Integer hearingId);

    HearingEntity getHearingById(Integer hearingId);

    void validateHearingExistsElseError(Integer hearingId);

    List<EventResponse> getEvents(Integer hearingId);

    List<Transcript> getTranscriptsByHearingId(Integer hearingId);

    List<Annotation> getAnnotationsByHearingId(Integer hearingId);

    void removeMediaLinkToHearing(Integer courtCaseId);
}