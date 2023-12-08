package uk.gov.hmcts.darts.audio.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.model.EnhancedMediaRequestInfo;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestStatus;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

@UtilityClass
public class GetAudioRequestResponseMapper {

    public GetAudioRequestResponse mapToAudioRequestSummary(EnhancedMediaRequestInfo enhancedMediaRequestInfo, TransformedMediaEntity transformedMedia) {
        var response = new GetAudioRequestResponse();
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
        String outputFilename = transformedMedia.getOutputFormat();
        if (outputFilename != null) {
            response.setOutputFormat(AudioRequestOutputFormat.fromValue(outputFilename));
        }

        return response;
    }

}
