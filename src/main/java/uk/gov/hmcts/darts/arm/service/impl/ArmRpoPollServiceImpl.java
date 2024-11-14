package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoPollService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@ConditionalOnProperty(
    value = "darts.automated.task.process-e2e-arm-rpo",
    havingValue = "true"
)
@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoPollServiceImpl implements ArmRpoPollService {

    private final ArmRpoApi armRpoApi;
    private final ArmApiService armApiService;
    private final ArmRpoService armRpoService;
    private final UserIdentity userIdentity;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    private List<File> tempProductionFiles = new ArrayList<>();

    @Override
    public void pollArmRpo() {

        try {
            var armRpoExecutionDetailEntity = getArmRpoExecutionDetailEntity();
            if (isNull(armRpoExecutionDetailEntity)) {
                log.warn("Unable to find armRpoExecutionDetailEntity to poll");
                return;
            }
            var bearerToken = armApiService.getArmBearerToken();
            if (isNull(bearerToken)) {
                log.warn("Unable to get bearer token to poll ARM RPO");
                return;
            }

            var executionId = armRpoExecutionDetailEntity.getId();
            var userAccount = userIdentity.getUserAccount();

            // step to call ARM RPO API to get the extended searches by matter
            armRpoApi.getExtendedSearchesByMatter(bearerToken, executionId, userAccount);
            List<MasterIndexFieldByRecordClassSchema> headerColumns = armRpoApi.getMasterIndexFieldByRecordClassSchema(
                bearerToken, executionId,
                ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                userAccount);

            // step to call ARM RPO API to create export based on search results table
            var createExportBasedOnSearchResultsTable = armRpoApi.createExportBasedOnSearchResultsTable(bearerToken, executionId, headerColumns,
                                                                                                        userAccount);
            if (createExportBasedOnSearchResultsTable) {
                // step to call ARM RPO API to get the extended productions by matter
                armRpoApi.getExtendedProductionsByMatter(bearerToken, executionId, userAccount);
                // step to call ARM RPO API to get the production output files
                var productionOutputFiles = armRpoApi.getProductionOutputFiles(bearerToken, executionId, userAccount);

                for (var productionExportFileId : productionOutputFiles) {
                    String productionExportFilename = generateProductionExportFilename(productionExportFileId);
                    // step to call ARM RPO API to download the production export file
                    var inputStream = armRpoApi.downloadProduction(bearerToken, executionId, productionExportFileId, userAccount);
                    log.info("About to save production export file to temp workspace {}", productionExportFilename);
                    Path tempProductionFile = fileOperationService.saveFileToTempWorkspace(
                        inputStream,
                        productionExportFilename,
                        armDataManagementConfiguration,
                        false
                    );
                    tempProductionFiles.add(tempProductionFile.toFile());
                }
                if (CollectionUtils.isNotEmpty(tempProductionFiles)) {
                    // step to call ARM RPO API to remove the production
                    armRpoApi.removeProduction(bearerToken, executionId, userAccount);
                    reconcile(tempProductionFiles, executionId);
                } else {
                    log.warn("No production export files found");
                }
            } else {
                log.warn("Create export of production files is still in progress");
            }

        } catch (Exception e) {
            log.error("Error while polling ARM RPO", e);
        } finally {
            try {
                cleanUpTempFiles();
            } catch (Exception e) {
                log.error("Error while cleaning up temp files", e);
            }

        }
    }

    private void reconcile(List<File> tempProductionFiles, Integer executionId) {
        // TODO this is to be implemented in ticket DMP-3619
    }

    private void cleanUpTempFiles() {
        for (var tempProductionFile : tempProductionFiles) {
            if (tempProductionFile.exists()) {
                if (tempProductionFile.delete()) {
                    log.info("Deleted temp production file {}", tempProductionFile.getName());
                } else {
                    log.warn("Failed to delete temp production file {}", tempProductionFile.getName());
                }
            }
        }
    }


    private String generateProductionExportFilename(String productionExportFileId) {
        return "productionExportFileId_" + productionExportFileId + ".csv";
    }

    private ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity() {
        // TODO this needs to be thought through how to handle the multiple states and will be played in another ticket
        return armRpoService.getLatestArmRpoExecutionDetailEntity();
    }
}
