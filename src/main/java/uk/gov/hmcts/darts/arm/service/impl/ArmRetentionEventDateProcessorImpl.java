package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
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
    public void calculateEventDates() {
        boolean updateRetention = true;
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities =
            externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(EodHelper.armLocation(), updateRetention);

        for (ExternalObjectDirectoryEntity externalObjectDirectory : externalObjectDirectoryEntities) {
            armRetentionEventDateCalculator.calculateRetentionEventDate(externalObjectDirectory.getId());
        }
    }

}
