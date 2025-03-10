package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmRetentionEventDateProcessorImpl implements ArmRetentionEventDateProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private final ArmRetentionEventDateCalculator armRetentionEventDateCalculator;

    @Override
    public void calculateEventDates(Integer batchSize) {
        boolean updateRetention = true;
        List<Integer> externalObjectDirectoryEntitiesIds =
            externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(EodHelper.armLocation(), updateRetention,
                                                                                           Limit.of(batchSize));

        for (Integer externalObjectDirectoryId : externalObjectDirectoryEntitiesIds) {
            try {
                armRetentionEventDateCalculator.calculateRetentionEventDate(externalObjectDirectoryId);
            } catch (Exception e) {
                log.error("Unable to calculate retention date for EOD {}", externalObjectDirectoryId, e);
            }
        }
    }

}
