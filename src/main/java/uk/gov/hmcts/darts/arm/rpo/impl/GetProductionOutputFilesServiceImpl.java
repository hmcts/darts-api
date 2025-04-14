package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoInProgressException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.GetProductionOutputFilesService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.IN_PROGRESS_STATUS;
import static uk.gov.hmcts.darts.arm.enums.ArmRpoResponseStatusCode.READY_STATUS;

@Service
@AllArgsConstructor
@Slf4j
public class GetProductionOutputFilesServiceImpl implements GetProductionOutputFilesService {

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;

    @Override
    public List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        log.info("getProductionOutputFiles called with executionId: {}", executionId);
        final ArmRpoExecutionDetailEntity executionDetail = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(executionDetail,
                                                 ArmRpoHelper.getProductionOutputFilesRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(),
                                                 userAccount);

        final StringBuilder exceptionMessageBuilder = new StringBuilder(135).append("ARM getProductionOutputFiles: ");

        String productionId = executionDetail.getProductionId();
        if (StringUtils.isBlank(productionId)) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("production id was blank for execution id: ")
                                                                 .append(executionId).toString(),
                                                             executionDetail, userAccount);
        }

        ProductionOutputFilesResponse productionOutputFilesResponse;
        try {
            productionOutputFilesResponse = armRpoClient.getProductionOutputFiles(bearerToken, createProductionOutputFilesRequest(productionId));
        } catch (FeignException e) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("API call failed: ").append(e).toString(),
                                                             executionDetail, userAccount);
        }
        log.info("ARM RPO Response - ProductionOutputFilesResponse: {}", productionOutputFilesResponse);
        return processProductionOutputFilesResponse(userAccount, productionOutputFilesResponse, exceptionMessageBuilder, executionDetail);
    }

    private List<String> processProductionOutputFilesResponse(UserAccountEntity userAccount, ProductionOutputFilesResponse productionOutputFilesResponse,
                                                              StringBuilder exceptionMessageBuilder,
                                                              ArmRpoExecutionDetailEntity executionDetail) {
        armRpoUtil.handleResponseStatus(userAccount, productionOutputFilesResponse, exceptionMessageBuilder, executionDetail);

        List<ProductionOutputFilesResponse.ProductionExportFile> productionExportFiles = productionOutputFilesResponse.getProductionExportFiles();
        if (CollectionUtils.isEmpty(productionExportFiles)
            || productionExportFiles.stream().anyMatch(Objects::isNull)) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("No production export files were returned").toString(),
                                                             executionDetail, userAccount);
        }

        validateProductionExportResponse(userAccount, exceptionMessageBuilder, executionDetail, productionExportFiles);

        List<String> productionExportFileIds = productionExportFiles.stream()
            .filter(Objects::nonNull)
            .map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .filter(Objects::nonNull)
            .filter(productionExportFileDetails -> productionExportFileDetails.getStatus() == READY_STATUS.getStatusCode())
            .map(ProductionOutputFilesResponse.ProductionExportFileDetail::getProductionExportFileId)
            .filter(StringUtils::isNotBlank)
            .toList();

        if (productionExportFileIds.isEmpty()) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("No production export file id's were returned").toString(),
                                                             executionDetail, userAccount);
        }

        armRpoService.updateArmRpoStatus(executionDetail, ArmRpoHelper.completedRpoStatus(), userAccount);

        return productionExportFileIds;
    }

    private void validateProductionExportResponse(UserAccountEntity userAccount, StringBuilder exceptionMessageBuilder,
                                                  ArmRpoExecutionDetailEntity executionDetail,
                                                  List<ProductionOutputFilesResponse.ProductionExportFile> productionExportFiles) {
        if (productionExportFiles.stream().map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .anyMatch(details -> nonNull(details) && IN_PROGRESS_STATUS.getStatusCode() == details.getStatus())) {
            throw new ArmRpoInProgressException("getProductionExportFileDetails", executionDetail.getId());
        } else if (productionExportFiles.stream().map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .allMatch(details -> nonNull(details) && READY_STATUS.getStatusCode() == details.getStatus())) {
            log.info("All production export files are ready for download");
        } else if (productionExportFiles.stream().map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .allMatch(details -> nonNull(details) && details.getStatus() != READY_STATUS.getStatusCode())) {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("Production export files contain failures").toString(),
                                                             executionDetail, userAccount);
        } else if (productionExportFiles.stream().map(ProductionOutputFilesResponse.ProductionExportFile::getProductionExportFileDetails)
            .anyMatch(details -> nonNull(details) && READY_STATUS.getStatusCode() == details.getStatus())) {
            log.warn("Some production export files are not ready for download");
        } else {
            throw armRpoUtil.handleFailureAndCreateException(exceptionMessageBuilder.append("Production export files contain failures").toString(),
                                                             executionDetail, userAccount);
        }
    }

    private ProductionOutputFilesRequest createProductionOutputFilesRequest(String productionId) {
        return ProductionOutputFilesRequest.builder()
            .productionId(productionId)
            .build();
    }

}
