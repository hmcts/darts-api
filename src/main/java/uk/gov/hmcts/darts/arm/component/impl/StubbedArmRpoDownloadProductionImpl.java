package uk.gov.hmcts.darts.arm.component.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.util.DateTimeHelper;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * This class is used to download production data from ARM RPO using the stubbed client. This client takes the eod ids in the header.
 */
@Component
@ConditionalOnProperty(prefix = "darts.storage.arm", name = "is-mock-arm-rpo-download-csv", havingValue = "true")
@AllArgsConstructor
@Slf4j
public class StubbedArmRpoDownloadProductionImpl implements ArmRpoDownloadProduction {

    public static final int MAX_EOD_RECORDS = 10;
    private final ArmClientService armClientService;
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArmRpoService armRpoService;

    @Override
    public feign.Response downloadProduction(String bearerToken, Integer executionId, String productionExportFileId) {
        log.info("Downloading stubbed production data");
        StringBuilder errorMessage = new StringBuilder(93).append("Unable to downloadProduction from ARM RPO");

        ArmAutomatedTaskEntity armAutomatedTaskEntity =
            armAutomatedTaskRepository.findByAutomatedTaskTaskName(AutomatedTaskName.PROCESS_E2E_ARM_PENDING_TASK_NAME.getTaskName())
                .orElseThrow(() -> new ArmRpoException(errorMessage.append(" - Unable to find ARM automated task").toString()));
        int rpoCsvStartHour = armAutomatedTaskEntity.getRpoCsvStartHour();
        int rpoCsvEndHour = armAutomatedTaskEntity.getRpoCsvEndHour();

        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        OffsetDateTime executionDateTime = DateTimeHelper.floorToMinutes(armRpoExecutionDetailEntity.getCreatedDateTime());

        Limit limit = Limit.of(MAX_EOD_RECORDS);
        var eods = externalObjectDirectoryRepository.findAllByStatusAndDataIngestionTsBetweenAndLimit(
            EodHelper.armRpoPendingStatus(),
            executionDateTime.minusHours(rpoCsvEndHour),
            executionDateTime.minusHours(rpoCsvStartHour),
            limit);

        if (eods.isEmpty()) {
            throw new ArmRpoException(errorMessage.append(" - No EODS found").toString());
        }

        String eodIds = eods.stream().map(eod -> eod.getId().toString()).collect(Collectors.joining(", "));
        return armClientService.downloadProduction(bearerToken, eodIds, productionExportFileId);

    }
}
