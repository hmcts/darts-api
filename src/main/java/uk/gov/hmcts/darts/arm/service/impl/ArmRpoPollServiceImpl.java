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
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
    private final LogApi logApi;

    private List<File> tempProductionFiles;

    private List<Integer> allowableFailedStates = null;


    @Override
    public void pollArmRpo(boolean isManualRun) {
        setupFailedStatuses();
        Integer executionId = null;
        tempProductionFiles = new ArrayList<>();
        try {
            var armRpoExecutionDetailEntity = getArmRpoExecutionDetailEntity(isManualRun);
            if (isNull(armRpoExecutionDetailEntity)) {
                log.warn("Unable to find armRpoExecutionDetailEntity to poll");
                logApi.armRpoPollingFailed(executionId);
                return;
            }
            var bearerToken = armApiService.getArmBearerToken();
            if (isNull(bearerToken)) {
                log.warn("Unable to get bearer token to poll ARM RPO");
                logApi.armRpoPollingFailed(executionId);
                return;
            }

            executionId = armRpoExecutionDetailEntity.getId();
            var userAccount = userIdentity.getUserAccount();

            // step to call ARM RPO API to get the extended searches by matter
            armRpoApi.getExtendedSearchesByMatter(bearerToken, executionId, userAccount);
            // step to call ARM RPO API to get the master index field by record class schema
            List<MasterIndexFieldByRecordClassSchema> headerColumns = armRpoApi.getMasterIndexFieldByRecordClassSchema(
                bearerToken, executionId,
                ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                userAccount);

            // step to call ARM RPO API to create export based on search results table
            boolean createExportBasedOnSearchResultsTable = armRpoApi.createExportBasedOnSearchResultsTable(bearerToken, executionId, headerColumns,
                                                                                                            userAccount);
            if (createExportBasedOnSearchResultsTable) {
                // step to call ARM RPO API to get the extended productions by matter
                armRpoApi.getExtendedProductionsByMatter(bearerToken, executionId, userAccount);
                // step to call ARM RPO API to get the production output files
                var productionOutputFiles = armRpoApi.getProductionOutputFiles(bearerToken, executionId, userAccount);

                for (var productionExportFileId : productionOutputFiles) {
                    String productionExportFilename = generateTempProductionExportFilename(productionExportFileId);
                    // step to call ARM RPO API to download the production export file
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
                    armRpoApi.removeProduction(bearerToken, executionId, userAccount);
                    log.debug("About to reconcile production files");
                    armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, tempProductionFiles);
                } else {
                    log.warn("No production export files found");
                }
            } else {
                log.warn("Create export of production files is still in progress");
            }
            logApi.armRpoPollingSuccessful(executionId);
        } catch (Exception e) {
            log.error("Error while polling ARM RPO", e);
            logApi.armRpoPollingFailed(executionId);
        } finally {
            try {
                cleanUpTempFiles();
            } catch (Exception e) {
                log.error("Error while cleaning up ARM RPO polling service temp files", e);
            }
        }
    }

    private void setupFailedStatuses() {
        if (CollectionUtils.isEmpty(allowableFailedStates)) {
            allowableFailedStates = Collections.unmodifiableList(List.of(
                ArmRpoHelper.getExtendedSearchesByMatterRpoState().getId(),
                ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState().getId(),
                ArmRpoHelper.createExportBasedOnSearchResultsTableRpoState().getId(),
                ArmRpoHelper.getExtendedProductionsByMatterRpoState().getId(),
                ArmRpoHelper.getProductionOutputFilesRpoState().getId(),
                ArmRpoHelper.downloadProductionRpoState().getId(),
                ArmRpoHelper.removeProductionRpoState().getId()
            ));
        }
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

    private String generateTempProductionExportFilename(String productionExportFileId) {
        return "productionExportFileId_" + productionExportFileId + ".csv";
    }

    private ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(boolean isManualRun) {
        var armRpoExecutionDetailEntity = armRpoService.getLatestArmRpoExecutionDetailEntity();
        if (isNull(armRpoExecutionDetailEntity)) {
            return null;
        }
        // If the previous state is saveBackgroundSearch and status is completed
        // or the previous state is createExportBasedOnSearchResultsTable and status is in progress, return the entity
        if (saveBackgroundSearchCompleted(armRpoExecutionDetailEntity)
            || createExportBasedOnSearchResultsTableInProgress(armRpoExecutionDetailEntity)) {
            return armRpoExecutionDetailEntity;
        }
        // If the job is a manual run, the previous status is failed and the state is greater than saveBackgroundSearch, return the entity
        if (isManualRun && pollServiceNotInProgress(armRpoExecutionDetailEntity)) {
            return armRpoExecutionDetailEntity;
        }
        return null;
    }


    private boolean pollServiceNotInProgress(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        if (nonNull(armRpoExecutionDetailEntity.getArmRpoState())
            && ArmRpoHelper.failedRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId())
            && allowableFailedStates.contains(armRpoExecutionDetailEntity.getArmRpoState().getId())) {
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
