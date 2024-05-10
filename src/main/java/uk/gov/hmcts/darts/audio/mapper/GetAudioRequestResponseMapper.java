package uk.gov.hmcts.darts.audio.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.model.EnhancedMediaRequestInfo;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponseV1;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestStatus;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

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

}
