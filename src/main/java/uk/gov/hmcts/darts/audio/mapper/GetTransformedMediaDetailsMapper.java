package uk.gov.hmcts.darts.audio.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponseCase;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponseCourthouse;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponseHearing;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponseMediaRequest;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

import java.util.ArrayList;
import java.util.List;

@Component
public class GetTransformedMediaDetailsMapper {
    public List<SearchTransformedMediaResponse> mapSearchResults(List<TransformedMediaEntity> entityList) {
        List<SearchTransformedMediaResponse> mappedDetails = new ArrayList<>();
        for (TransformedMediaEntity entity : entityList) {
            SearchTransformedMediaResponse transformedMediaDetails = new SearchTransformedMediaResponse();
            transformedMediaDetails.setId(entity.getId());
            transformedMediaDetails.setFileName(entity.getOutputFilename());

            if (entity.getOutputFormat() != null) {
                transformedMediaDetails.setFileFormat(entity.getOutputFormat().name());
            }

            transformedMediaDetails.setLastAccessedAt(entity.getLastAccessed());
            transformedMediaDetails.setFileSizeBytes(entity.getOutputFilesize());

            if (entity.getMediaRequest() != null) {
                SearchTransformedMediaResponseMediaRequest mediaRequest = new SearchTransformedMediaResponseMediaRequest();
                mediaRequest.setId(entity.getMediaRequest().getId());
                mediaRequest.setRequestedAt(entity.getMediaRequest().getCreatedDateTime());
                mediaRequest.setOwnerUserId(entity.getMediaRequest().getCurrentOwner().getId());
                mediaRequest.setRequestedByUserId(entity.getMediaRequest().getRequestor().getId());
                transformedMediaDetails.setMediaRequest(mediaRequest);
            }

            if (entity.getMediaRequest() != null && entity.getMediaRequest().getHearing() != null
                && entity.getMediaRequest().getHearing().getCourtCase() != null) {
                SearchTransformedMediaResponseCase caseResponse = new SearchTransformedMediaResponseCase();
                caseResponse.setId(entity.getMediaRequest().getHearing().getCourtCase().getId());
                caseResponse.setCaseNumber(entity.getMediaRequest().getHearing().getCourtCase().getCaseNumber());
                transformedMediaDetails.setCase(caseResponse);
            }

            if (entity.getMediaRequest() != null && entity.getMediaRequest().getHearing() != null
                && entity.getMediaRequest().getHearing().getCourtroom() != null) {
                SearchTransformedMediaResponseCourthouse courthouseResponse = new SearchTransformedMediaResponseCourthouse();
                courthouseResponse.setId(entity.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId());
                courthouseResponse.setDisplayName(entity.getMediaRequest().getHearing().getCourtroom().getCourthouse().getDisplayName());
                transformedMediaDetails.setCourthouse(courthouseResponse);
            }

            if (entity.getMediaRequest() != null && entity.getMediaRequest().getHearing() != null) {
                SearchTransformedMediaResponseHearing hearingResponse = new SearchTransformedMediaResponseHearing();
                hearingResponse.setId(entity.getMediaRequest().getHearing().getId());
                hearingResponse.setHearingDate(entity.getMediaRequest().getHearing().getHearingDate());
                transformedMediaDetails.setHearing(hearingResponse);
            }

            mappedDetails.add(transformedMediaDetails);
        }

        return mappedDetails;
    }
}