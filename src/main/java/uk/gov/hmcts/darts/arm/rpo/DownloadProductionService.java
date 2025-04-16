package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface DownloadProductionService {

    InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileId, UserAccountEntity userAccount) throws IOException;

}
