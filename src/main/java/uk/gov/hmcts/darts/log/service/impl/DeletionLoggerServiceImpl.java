package uk.gov.hmcts.darts.log.service.impl;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.log.service.DeletionLoggerService;

@Service
@NoArgsConstructor
@Slf4j
public class DeletionLoggerServiceImpl implements DeletionLoggerService {

    @Override
    public void mediaDeleted(Long mediaId) {
        log.info("Media deleted: med_id={}", mediaId);
    }

    @Override
    public void transcriptionDeleted(Long transcriptionId) {
        log.info("Transcript deleted: trd_id={}", transcriptionId);
    }
}
