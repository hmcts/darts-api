package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArmRpoDownloadProduction;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.DownloadProductionService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.isNull;

@Service
@AllArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.PreserveStackTrace"})
public class DownloadProductionServiceImpl implements DownloadProductionService {

    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;
    private final ArmRpoDownloadProduction armRpoDownloadProduction;

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileId,
                                          UserAccountEntity userAccount) throws IOException {
        log.info("downloadProduction called with executionId: {}, productionExportFileId: {}", executionId, productionExportFileId);
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.downloadProductionRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        feign.Response response;
        StringBuilder errorMessage = new StringBuilder(185).append("Failure during download production: ");

        try {
            response = armRpoDownloadProduction.downloadProduction(bearerToken, executionId, productionExportFileId);
        } catch (FeignException feignException) {
            log.error(errorMessage.append("Error during ARM RPO download production id: ").append(productionExportFileId)
                          .append(feignException.getMessage()).toString(), feignException);
            int status = feignException.status();
            // If unauthorized or forbidden, retry once with a refreshed token
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                try {
                    String refreshedBearer = armRpoUtil.retryGetBearerToken("downloadProduction");
                    response = armRpoDownloadProduction.downloadProduction(refreshedBearer, executionId, productionExportFileId);
                } catch (FeignException retryEx) {
                    throw armRpoUtil.handleFailureAndCreateException(
                        errorMessage.append("API call failed after retry: ").append(retryEx.getMessage()).toString(),
                        armRpoExecutionDetailEntity, userAccount);
                }
            } else {
                throw armRpoUtil.handleFailureAndCreateException(errorMessage.append("API call failed: ").append(feignException.getMessage()).toString(),
                                                                 armRpoExecutionDetailEntity, userAccount);
            }
        }
        log.info("ARM RPO Response - downloadProduction response: {}", response);

        // on any error occurring, return a download failure
        if (isNull(response) || !HttpStatus.valueOf(response.status()).is2xxSuccessful()) {
            errorMessage.append("Failed ARM RPO download production with id: ").append(productionExportFileId)
                .append(" response ").append(response);
            log.error(errorMessage.toString());
            throw armRpoUtil.handleFailureAndCreateException(errorMessage.toString(), armRpoExecutionDetailEntity, userAccount);
        }

        log.info("Successfully downloaded ARM data for productionExportFileId: {}", productionExportFileId);
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
        return response.body().asInputStream();
    }
}
