package uk.gov.hmcts.darts.audio.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

public interface AudioUploadService {
    void addAudio(MultipartFile audioFileStream, AddAudioMetadataRequest addAudioMetadataRequest);

    void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity mediaEntity);

    void linkAudioToHearingByEvent(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);
}