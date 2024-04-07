package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmRetentionEventDateProcessorImpl implements ArmRetentionEventDateProcessor {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private final ArmRetentionEventDateCalculator armRetentionEventDateCalculator;
    private final boolean UPDATE_RETENTION = true;

    @Override
    public void calculateEventDates() {
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities =
            externalObjectDirectoryRepository.findAllByExternalLocationTypeAndUpdateRetention(EodHelper.armLocation(), UPDATE_RETENTION);

        for (ExternalObjectDirectoryEntity externalObjectDirectory : externalObjectDirectoryEntities) {
            armRetentionEventDateCalculator.calculateRetentionEventDate(externalObjectDirectory.getId());
        }
    }

    private ExternalLocationTypeEntity getEodSourceLocation() {
        var armClient = armDataManagementConfiguration.getArmClient();
        if (armClient.equalsIgnoreCase("darts")) {
            return EodHelper.unstructuredLocation();
        } else if (armClient.equalsIgnoreCase("dets")) {
            return EodHelper.detsLocation();
        } else {
            throw new DartsException(String.format("Invalid arm client '%s'", armClient));
        }
    }
}
