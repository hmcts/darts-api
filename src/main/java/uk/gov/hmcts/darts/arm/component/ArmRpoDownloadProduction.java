package uk.gov.hmcts.darts.arm.component;

public interface ArmRpoDownloadProduction {

    feign.Response downloadProduction(String bearerToken, Integer executionId, String productionExportFileId);
}
