package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
import static java.util.Objects.nonNull;

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

    private List<File> tempProductionFiles;

    @Override
    public void pollArmRpo(boolean isManualRun) {
        tempProductionFiles = new ArrayList<>();
        try {
            var armRpoExecutionDetailEntity = getArmRpoExecutionDetailEntity(isManualRun);
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
            log.debug("About to call getExtendedSearchesByMatter");
            armRpoApi.getExtendedSearchesByMatter(bearerToken, executionId, userAccount);
            // step to call ARM RPO API to get the master index field by record class schema
            log.debug("About to call getMasterIndexFieldByRecordClassSchema");
            List<MasterIndexFieldByRecordClassSchema> headerColumns = armRpoApi.getMasterIndexFieldByRecordClassSchema(
                bearerToken, executionId,
                ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                userAccount);

            // step to call ARM RPO API to create export based on search results table
            log.debug("About to call createExportBasedOnSearchResultsTable");
            var createExportBasedOnSearchResultsTable = armRpoApi.createExportBasedOnSearchResultsTable(bearerToken, executionId, headerColumns,
                                                                                                        userAccount);
            if (createExportBasedOnSearchResultsTable) {
                // step to call ARM RPO API to get the extended productions by matter
                log.debug("About to call getExtendedProductionsByMatter");
                armRpoApi.getExtendedProductionsByMatter(bearerToken, executionId, userAccount);
                // step to call ARM RPO API to get the production output files
                log.debug("About to call getProductionOutputFiles");
                var productionOutputFiles = armRpoApi.getProductionOutputFiles(bearerToken, executionId, userAccount);

                for (var productionExportFileId : productionOutputFiles) {
                    String productionExportFilename = generateProductionExportFilename(productionExportFileId);
                    // step to call ARM RPO API to download the production export file
                    log.debug("About to call downloadProduction");
                    var inputStream = armRpoApi.downloadProduction(bearerToken, executionId, productionExportFileId, userAccount);
                    log.info("About to save production export file to temp workspace {}", productionExportFilename);
                    Path tempProductionFile = fileOperationService.saveFileToTempWorkspace(
                        inputStream,
                        productionExportFilename,
                        armDataManagementConfiguration,
                        true
                    );
                    tempProductionFiles.add(tempProductionFile.toFile());
                }
                if (CollectionUtils.isNotEmpty(tempProductionFiles)) {
                    // step to call ARM RPO API to remove the production
                    log.debug("About to call removeProduction");
                    armRpoApi.removeProduction(bearerToken, executionId, userAccount);
                    log.debug("About to reconcile production files");
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
                log.error("Error while cleaning up ARM RPO polling service temp files", e);
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

    private ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(boolean isManualRun) {
        var armRpoExecutionDetailEntity = armRpoService.getLatestArmRpoExecutionDetailEntity();
        if (isNull(armRpoExecutionDetailEntity)) {
            return null;
        }

        if (saveBackgroundSearchCompleted(armRpoExecutionDetailEntity)
            || createExportBasedOnSearchResultsTableInProgress(armRpoExecutionDetailEntity)) {
            return armRpoExecutionDetailEntity;
        }
        if (isManualRun && pollServiceNotInProgress(armRpoExecutionDetailEntity)) {
            return armRpoExecutionDetailEntity;
        }
        return null;
    }


    private boolean pollServiceNotInProgress(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        if (nonNull(armRpoExecutionDetailEntity.getArmRpoState())
            && ArmRpoHelper.failedRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId())
            && armRpoExecutionDetailEntity.getArmRpoState().getId() >= ArmRpoHelper.getExtendedSearchesByMatterRpoState().getId()) {
            return true;
        }
        return false;
    }

    private boolean createExportBasedOnSearchResultsTableInProgress(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return nonNull(armRpoExecutionDetailEntity.getArmRpoState())
            && ArmRpoHelper.createExportBasedOnSearchResultsTableRpoState().getId().equals(armRpoExecutionDetailEntity.getArmRpoState().getId())
            && ArmRpoHelper.inProgressRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId());
    }

    private boolean saveBackgroundSearchCompleted(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return nonNull(armRpoExecutionDetailEntity.getArmRpoState())
            && ArmRpoHelper.saveBackgroundSearchRpoState().getId().equals(armRpoExecutionDetailEntity.getArmRpoState().getId())
            && ArmRpoHelper.completedRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId());
    }
}
