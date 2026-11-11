package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.service.FindFilesService;
import uk.gov.hmcts.darts.common.entity.JobLastSuccessfulRunTimeEntity;
import uk.gov.hmcts.darts.common.repository.JobLastSuccessfulRunTimeRepository;

import java.time.Clock;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class FindFilesServiceImpl implements FindFilesService {

    private static final String JOB_NAME = "find_files";

    private final JobLastSuccessfulRunTimeRepository jobLastSuccessfulRunTimeRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void findFiles(int batchSize) {
        Instant jobStartTime = clock.instant();
        JobLastSuccessfulRunTimeEntity lastRun = jobLastSuccessfulRunTimeRepository.findById(JOB_NAME)
            .orElseGet(() -> {
                JobLastSuccessfulRunTimeEntity newLastRun = new JobLastSuccessfulRunTimeEntity();
                newLastRun.setJobName(JOB_NAME);
                return newLastRun;
            });
        Instant lastSuccessfulRunTime = lastRun.getLastSuccessfulRunTimestampUtc();
        log.info("FindFilesFromCVPAndCAR job: current time UTC {} ", jobStartTime);
        log.info("FindFilesFromCVPAndCAR job: last successful run time UTC {} ", lastSuccessfulRunTime == null ? "none" : lastSuccessfulRunTime);
        log.info("FindFilesFromCVPAndCAR job: batch size {} ", batchSize);

        // Future file discovery belongs here, before advancing the successful run timestamp.

        lastRun.setLastSuccessfulRunTimestampUtc(jobStartTime);
        jobLastSuccessfulRunTimeRepository.save(lastRun);
    }
}
