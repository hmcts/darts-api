package uk.gov.hmcts.darts.arm.component;

public interface ArmRpoDownloadProduction {

    feign.Response downloadProduction(String bearerToken, String productionExportFileId);
}
