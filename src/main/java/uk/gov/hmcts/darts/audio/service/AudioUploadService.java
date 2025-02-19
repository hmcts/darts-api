package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

public interface AudioUploadService {

    void addAudio(String guid, AddAudioMetadataRequest addAudioMetadataRequest);

    void deleteUploadedAudio(String guid);

    void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity mediaEntity);
}