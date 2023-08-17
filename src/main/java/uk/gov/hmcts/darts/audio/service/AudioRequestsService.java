package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.service.impl.AudioRequestSummaryResult;

import java.util.List;

public interface AudioRequestsService {

    List<AudioRequestSummaryResult> viewAudioRequests(Integer userId, Boolean expired);

}
