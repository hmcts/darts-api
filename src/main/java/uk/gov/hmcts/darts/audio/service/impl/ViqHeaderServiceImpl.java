package uk.gov.hmcts.darts.audio.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.audio.service.ViqHeaderService;

@Slf4j
@Service
public class ViqHeaderServiceImpl implements ViqHeaderService {

    private static final String VIQ_HEADER_SERVICE_NOT_SUPPORTED_MESSAGE = "VIQ Header Service not yet implemented";

    @Override
    public String generatePlaylist(Integer hearingId, String startTime, String fileLocation) {
        log.info("About to generate playlist");
        throw new NotImplementedException(VIQ_HEADER_SERVICE_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public String generateAnnotation(Integer hearingId, String startTime, String endTime) {
        throw new NotImplementedException(VIQ_HEADER_SERVICE_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public String generateReadme(ViqMetaData viqMetaData) {
        throw new NotImplementedException(VIQ_HEADER_SERVICE_NOT_SUPPORTED_MESSAGE);
    }
}
