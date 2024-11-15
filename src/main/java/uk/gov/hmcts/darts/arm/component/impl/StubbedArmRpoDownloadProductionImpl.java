package uk.gov.hmcts.darts.arm.component.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;

@Component
@ConditionalOnProperty(prefix = "darts.storage.arm", name = "is_mock_arm_rpo_download_csv", havingValue = "true")
@AllArgsConstructor
@Slf4j
/**
 * This class is used to download production data from ARM RPO using the stubbed client. This client takes the eod ids in the header.
 */
public class StubbedArmRpoDownloadProductionImpl implements ArmRpoDownloadProduction {

    private static final String EOD_REQUEST_HEADER = "EOD_IDS";
    private final ArmRpoClient armRpoClient;


    @Override
    public feign.Response downloadProduction(String bearerToken, String productionExportFileId) {
        log.info("Downloading production data");
        String eods = "10";
        return armRpoClient.downloadProduction(bearerToken, eods, productionExportFileId);
    }
}
