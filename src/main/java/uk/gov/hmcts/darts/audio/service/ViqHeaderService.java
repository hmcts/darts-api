package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.model.ViqMetaData;

public interface ViqHeaderService {

    String generatePlaylist(Integer hearingId, String startTime, String fileLocation);

    String generateAnnotation(Integer hearingId, String startTime, String endTime);

    String generateReadme(ViqMetaData viqMetaData);
}
