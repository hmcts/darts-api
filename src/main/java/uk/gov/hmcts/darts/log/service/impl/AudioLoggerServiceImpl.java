package uk.gov.hmcts.darts.log.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.log.service.AudioLoggerService;
import uk.gov.hmcts.darts.util.DataUtil;

import static uk.gov.hmcts.darts.util.DateTimeHelper.getDateTimeIsoFormatted;

@Service
@Slf4j
@AllArgsConstructor
public class AudioLoggerServiceImpl implements AudioLoggerService {

    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void audioUploaded(AddAudioMetadataRequest request) {
        log.info("Audio uploaded: courthouse={}, courtroom={}, started_at={}, ended_at={}",
                 DataUtil.toUpperCase(request.getCourthouse()),
                 DataUtil.toUpperCase(request.getCourtroom()),
                 getDateTimeIsoFormatted(request.getStartedAt()),
                 getDateTimeIsoFormatted(request.getEndedAt()));
    }

    @Override
    public void missingCourthouse(String courthouse, String courtroom) {
        log.warn("Courthouse not found: courthouse={}, courtroom={}, timestamp={}",
                 DataUtil.toUpperCase(courthouse),
                 DataUtil.toUpperCase(courtroom),
                 getDateTimeIsoFormatted(currentTimeHelper.currentOffsetDateTime()));
    }
}
