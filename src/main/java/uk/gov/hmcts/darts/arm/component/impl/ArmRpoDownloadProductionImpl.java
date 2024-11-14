package uk.gov.hmcts.darts.arm.component.impl;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;

@ConditionalOnProperty(
    value = "darts.storage.arm.is_mock_arm_rpo_download_csv",
    havingValue = "false"
)
@Component
@AllArgsConstructor
public class ArmRpoDownloadProductionImpl implements ArmRpoDownloadProduction {

    private final ArmRpoClient armRpoClient;

    @Override
    public feign.Response downloadProduction(String bearerToken, String productionExportFileId) {
        return armRpoClient.downloadProduction(bearerToken, productionExportFileId);
    }
}
