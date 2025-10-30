package uk.gov.hmcts.darts.arm.component.impl;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;
import uk.gov.hmcts.darts.arm.service.ArmClientService;

@Component
@ConditionalOnProperty(prefix = "darts.storage.arm", name = "is-mock-arm-rpo-download-csv", havingValue = "false")
@AllArgsConstructor
public class ArmRpoDownloadProductionImpl implements ArmRpoDownloadProduction {

    private final ArmClientService armClientService;

    @Override
    public feign.Response downloadProduction(String bearerToken, Integer executionId, String productionExportFileId) {
        return armClientService.downloadProduction(bearerToken, productionExportFileId);
    }
}
