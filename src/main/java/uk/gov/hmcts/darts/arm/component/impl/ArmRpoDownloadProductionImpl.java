package uk.gov.hmcts.darts.arm.component.impl;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;

@Component
@ConditionalOnProperty(prefix = "darts.storage.arm", name = "is-mock-arm-rpo-download-csv", havingValue = "false")
@AllArgsConstructor
public class ArmRpoDownloadProductionImpl implements ArmRpoDownloadProduction {

    private final ArmRpoClient armRpoClient;

    @Override
    public feign.Response downloadProduction(String bearerToken, Integer executionId, String productionExportFileId) {
        return armRpoClient.downloadProduction(bearerToken, productionExportFileId);
    }
}
