package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audiorecording.model.AddAudioRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;

public interface AudioService {

    InputStream download(Integer mediaRequestId);

    InputStream preview(Integer mediaId);

    void addAudio(AddAudioRequest addAudioRequest);

    void linkAudioAndHearing(AddAudioRequest addAudioRequest, MediaEntity savedMedia);
}
