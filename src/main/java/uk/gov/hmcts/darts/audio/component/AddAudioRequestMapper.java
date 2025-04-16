package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@FunctionalInterface
public interface AddAudioRequestMapper {
    MediaEntity mapToMedia(AddAudioMetadataRequest addAudioMetadataRequest, UserAccountEntity userAccount);
}
