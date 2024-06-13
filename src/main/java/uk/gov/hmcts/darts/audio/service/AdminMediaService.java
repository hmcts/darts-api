package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;

public interface AdminMediaService {

    AdminMediaResponse getMediasById(Integer id);

}
