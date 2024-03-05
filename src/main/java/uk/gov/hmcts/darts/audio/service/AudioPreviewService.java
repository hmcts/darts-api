package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AudioPreview;

public interface AudioPreviewService {

    AudioPreview getOrCreateAudioPreview(Integer mediaId);

}
