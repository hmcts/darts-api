package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.service.impl.AudioRequestSummaryResult;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

public interface MediaRequestService {

    MediaRequestEntity getMediaRequestById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, AudioRequestStatus audioRequestStatus);

    Integer saveAudioRequest(AudioRequestDetails audioRequestDetails);

    List<AudioRequestSummaryResult> viewAudioRequests(UserAccountEntity userAccountEntity,
                                                      Boolean expired);

}
