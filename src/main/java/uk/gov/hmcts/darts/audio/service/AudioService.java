package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AddAudioMetaDataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;

public interface AudioService {

    InputStream download(Integer mediaRequestId);

    InputStream preview(Integer mediaId);

    void addAudio(AddAudioMetaDataRequest addAudioMetaDataRequest);

    void linkAudioAndHearing(AddAudioMetaDataRequest addAudioMetaDataRequest, MediaEntity savedMedia);
}
