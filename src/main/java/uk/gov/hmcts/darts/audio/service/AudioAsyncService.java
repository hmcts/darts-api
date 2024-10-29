package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface AudioAsyncService {

    void linkAudioToHearingByEvent(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia, UserAccountEntity userAccount);
}
