package uk.gov.hmcts.darts.audio.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.model.EnhancedMediaRequestInfo;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaResponseCase;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaResponseCourthouse;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaResponseHearing;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaResponseMediaRequest;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponseV1;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestStatus;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UtilityClass
public class GetAudioRequestResponseMapper {

    public GetAudioRequestResponseV1 mapToAudioRequestSummary(EnhancedMediaRequestInfo enhancedMediaRequestInfo, TransformedMediaEntity transformedMedia) {
        var response = new GetAudioRequestResponseV1();
        response.setMediaRequestId(enhancedMediaRequestInfo.getMediaRequestId());
        response.setCaseId(enhancedMediaRequestInfo.getCaseId());
        response.setHearingId(enhancedMediaRequestInfo.getHearingId());
        response.setRequestType(enhancedMediaRequestInfo.getRequestType());
        response.setCaseNumber(enhancedMediaRequestInfo.getCaseNumber());
        response.setCourthouseName(enhancedMediaRequestInfo.getCourthouseName());
        response.setHearingDate(enhancedMediaRequestInfo.getHearingDate());
        response.setMediaRequestStartTs(enhancedMediaRequestInfo.getMediaRequestStartTs());
        response.setMediaRequestEndTs(enhancedMediaRequestInfo.getMediaRequestEndTs());
        response.setMediaRequestExpiryTs(transformedMedia.getExpiryTime());
        response.setMediaRequestStatus(MediaRequestStatus.fromValue(enhancedMediaRequestInfo.getMediaRequestStatus().toString()));
        response.setLastAccessedTs(transformedMedia.getLastAccessed());
        response.setOutputFilename(transformedMedia.getOutputFilename());
        uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat outputFormat = transformedMedia.getOutputFormat();
        if (outputFormat != null) {
            response.setOutputFormat(AudioRequestOutputFormat.fromValue(outputFormat.toString().toUpperCase(Locale.getDefault())));
        }

        return response;
    }

    public List<SearchTransformedMediaResponse> mapSearchResults(List<TransformedMediaEntity> entityList) {
        List<SearchTransformedMediaResponse> mappedDetails = new ArrayList<>();
        for (TransformedMediaEntity entity : entityList) {
            SearchTransformedMediaResponse transformedMediaDetails = new SearchTransformedMediaResponse();
            transformedMediaDetails.setId(entity.getId());
            transformedMediaDetails.setFileName(entity.getOutputFilename());
            transformedMediaDetails.setFileFormat(entity.getOutputFormat().name());
            transformedMediaDetails.setLastAccessedAt(entity.getLastAccessed());
            transformedMediaDetails.setFileSizeBytes(entity.getOutputFilesize());

            SearchTransformedMediaResponseMediaRequest mediaRequest = new SearchTransformedMediaResponseMediaRequest();
            mediaRequest.setId(entity.getMediaRequest().getId());
            mediaRequest.setRequestedAt(entity.getCreatedDateTime());
            mediaRequest.setOwnerUserId(entity.getMediaRequest().getCurrentOwner().getId());
            mediaRequest.setRequestedByUserId(entity.getMediaRequest().getCreatedBy().getId());
            transformedMediaDetails.setMediaRequest(mediaRequest);

            SearchTransformedMediaResponseCase caseResponse = new SearchTransformedMediaResponseCase();
            caseResponse.setId(entity.getMediaRequest().getHearing().getCourtCase().getId());
            caseResponse.setCaseNumber(entity.getMediaRequest().getHearing().getCourtCase().getCaseNumber());
            transformedMediaDetails.setCase(caseResponse);

            SearchTransformedMediaResponseCourthouse courthouseResponse = new SearchTransformedMediaResponseCourthouse();
            courthouseResponse.setId(entity.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId());
            courthouseResponse.setDisplayName(entity.getMediaRequest().getHearing().getCourtroom().getCourthouse().getDisplayName());
            transformedMediaDetails.setCourthouse(courthouseResponse);


            SearchTransformedMediaResponseHearing hearingResponse = new SearchTransformedMediaResponseHearing();
            hearingResponse.setId(entity.getMediaRequest().getHearing().getId());
            hearingResponse.setHearingDate(entity.getMediaRequest().getHearing().getHearingDate());
            transformedMediaDetails.setHearing(hearingResponse);

            mappedDetails.add(transformedMediaDetails);
        }

        return mappedDetails;
    }
}