package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.task.config.ArmRetentionEventDateCalculatorAutomatedTaskConfig;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.util.List;
import java.util.concurrent.Callable;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmRetentionEventDateProcessorImpl implements ArmRetentionEventDateProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private final ArmRetentionEventDateCalculator armRetentionEventDateCalculator;
    private final ArmRetentionEventDateCalculatorAutomatedTaskConfig automatedTaskConfigurationProperties;

    @Override
    @SuppressWarnings("PMD.DoNotUseThreads")//Required to handle InterruptedException
    public void calculateEventDates(Integer batchSize) {
        final boolean updateRetention = true;
        List<Long> externalObjectDirectoryEntitiesIds =
            externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(EodHelper.armLocation(), updateRetention,
                                                                                           Limit.of(batchSize));

        log.info("Processing {} EODs for retention event date calculation for batch size {}", externalObjectDirectoryEntitiesIds.size(), batchSize);

        try {
            List<Callable<Void>> tasks = externalObjectDirectoryEntitiesIds.stream()
                .map(externalObjectDirectoryId -> (Callable<Void>) () -> {
                    try {
                        armRetentionEventDateCalculator.calculateRetentionEventDate(externalObjectDirectoryId);
                    } catch (Exception e) {
                        log.error("Unable to calculate retention date for EOD {}", externalObjectDirectoryId, e);
                    }
                    return null;
                })
                .toList();
            AsyncUtil.invokeAllAwaitTermination(tasks, automatedTaskConfigurationProperties);
        } catch (InterruptedException e) {
            log.error("ArmRetentionEventDateProcessor interrupted exception", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("ArmRetentionEventDateProcessorImpl unexpected exception", e);
        }
    }

}
