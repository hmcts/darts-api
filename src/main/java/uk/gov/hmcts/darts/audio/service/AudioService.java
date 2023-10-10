package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;

public interface AudioService {

    InputStream preview(Integer mediaId);

    void addAudio(AddAudioMetadataRequest addAudioMetadataRequest);

    void linkAudioAndHearing(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);
}
