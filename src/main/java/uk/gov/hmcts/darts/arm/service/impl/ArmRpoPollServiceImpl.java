package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.exception.ArmRpoInProgressException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoPollService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.io.IOException;
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
    private final LogApi logApi;
    private final ArmRpoUtil armRpoUtil;

    private List<File> tempProductionFiles;

    private List<Integer> allowableFailedStates;
    private List<Integer> allowableInProgressStates;

    @Override
    public void pollArmRpo(boolean isManualRun) {
        log.info("Polling ARM RPO service - isManualRun: {}", isManualRun);
        setupFailedStatuses();
        setupAllowableInProgressStates();
        Integer executionId = null;
        tempProductionFiles = new ArrayList<>();
        try {
            var armRpoExecutionDetailEntity = getArmRpoExecutionDetailEntity(isManualRun);
            if (isNull(armRpoExecutionDetailEntity)) {
                log.warn("Unable to find latest armRpoExecutionDetailEntity in a valid state to poll");
                return;
            }
            executionId = armRpoExecutionDetailEntity.getId();

            var bearerToken = armApiService.getArmBearerToken();
            if (isNull(bearerToken)) {
                log.warn("Unable to get bearer token to poll ARM RPO");
                logApi.armRpoPollingFailed(executionId);
                return;
            }

            var userAccount = userIdentity.getUserAccount();

            // step to call ARM RPO API to get the extended searches by matter
            String productionName = armRpoApi.getExtendedSearchesByMatter(bearerToken, executionId, userAccount);

            String uniqueProductionName = armRpoUtil.generateUniqueProductionName(productionName);

            // step to call ARM RPO API to get the master index field by record class schema
            List<MasterIndexFieldByRecordClassSchema> headerColumns = armRpoApi.getMasterIndexFieldByRecordClassSchema(
                bearerToken, executionId,
                ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                userAccount);

            // step to call ARM RPO API to create export based on search results table
            boolean createExportBasedOnSearchResultsTable = armRpoApi.createExportBasedOnSearchResultsTable(
                bearerToken, executionId, headerColumns, uniqueProductionName, userAccount);
            if (createExportBasedOnSearchResultsTable) {
                processProductions(bearerToken, executionId, uniqueProductionName, userAccount, armRpoExecutionDetailEntity);
            } else {
                log.warn("ARM RPO Polling is still in-progress for createExportBasedOnSearchResultsTable");
            }
            log.info("Polling ARM RPO service completed");
        } catch (ArmRpoInProgressException e) {
            log.warn("ARM RPO Polling is still in-progress - {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error while polling ARM RPO", e);
            logApi.armRpoPollingFailed(executionId);
        } finally {
            cleanUpTempFiles();
        }
    }


    private void processProductions(String bearerToken, Integer executionId, String uniqueProductionName, UserAccountEntity userAccount,
                                    ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) throws IOException {
        // step to call ARM RPO API to get the extended productions by matter
        boolean getExtendedProductionsByMatter = armRpoApi.getExtendedProductionsByMatter(bearerToken, executionId, uniqueProductionName, userAccount);
        if (getExtendedProductionsByMatter) {
            // step to call ARM RPO API to get the production output files
            var productionOutputFiles = armRpoApi.getProductionOutputFiles(bearerToken, executionId, userAccount);

            for (var productionExportFileId : productionOutputFiles) {
                processProductionFiles(productionExportFileId, bearerToken, executionId, userAccount);
            }
            if (CollectionUtils.isNotEmpty(tempProductionFiles)) {
                // step to call ARM RPO API to remove the production
                armRpoApi.removeProduction(bearerToken, executionId, userAccount);
                log.debug("About to reconcile production files");
                armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, tempProductionFiles);
            } else {
                log.warn("No production export files found");
            }
            logApi.armRpoPollingSuccessful(executionId);
        } else {
            log.warn("ARM RPO Polling is still in-progress for getExtendedProductionsByMatter");
        }
    }

    private void processProductionFiles(String productionExportFileId, String bearerToken, Integer executionId,
                                        UserAccountEntity userAccount) throws IOException {
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

    private void setupFailedStatuses() {
        if (CollectionUtils.isEmpty(allowableFailedStates)) {
            allowableFailedStates = List.of(
                ArmRpoHelper.getExtendedSearchesByMatterRpoState().getId(),
                ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState().getId(),
                ArmRpoHelper.createExportBasedOnSearchResultsTableRpoState().getId(),
                ArmRpoHelper.getExtendedProductionsByMatterRpoState().getId(),
                ArmRpoHelper.getProductionOutputFilesRpoState().getId(),
                ArmRpoHelper.downloadProductionRpoState().getId(),
                ArmRpoHelper.removeProductionRpoState().getId()
            );
        }
    }

    private void setupAllowableInProgressStates() {
        if (CollectionUtils.isEmpty(allowableInProgressStates)) {
            allowableInProgressStates = List.of(
                ArmRpoHelper.getExtendedSearchesByMatterRpoState().getId(),
                ArmRpoHelper.createExportBasedOnSearchResultsTableRpoState().getId(),
                ArmRpoHelper.getExtendedProductionsByMatterRpoState().getId(),
                ArmRpoHelper.getProductionOutputFilesRpoState().getId()
            );
        }
    }

    private void cleanUpTempFiles() {
        try {
            for (var tempProductionFile : tempProductionFiles) {
                if (tempProductionFile.exists()) {
                    if (tempProductionFile.delete()) {
                        log.info("Deleted temp production file {}", tempProductionFile.getName());
                    } else {
                        log.warn("Failed to delete temp production file {}", tempProductionFile.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while cleaning up ARM RPO polling service temp files", e);
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
        // or the previous state is getExtendedSearchesByMatterId and status is in progress
        // or the previous state is createExportBasedOnSearchResultsTable and status is in progress
        // or the previous state is getExtendedProductionsByMatter and status is in progress
        // or the previous state is getProductionOutputFiles and status is in progress
        if (saveBackgroundSearchCompleted(armRpoExecutionDetailEntity)
            || pollServiceInProgress(armRpoExecutionDetailEntity)) {
            return armRpoExecutionDetailEntity;
        }
        // If the job is a manual run, the previous status is failed and the state is greater than saveBackgroundSearch, return the entity
        if (isManualRun && pollServiceFailed(armRpoExecutionDetailEntity)) {
            return armRpoExecutionDetailEntity;
        }
        return null;
    }

    private boolean pollServiceFailed(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return nonNull(armRpoExecutionDetailEntity.getArmRpoState())
            && ArmRpoHelper.failedRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId())
            && allowableFailedStates.contains(armRpoExecutionDetailEntity.getArmRpoState().getId());
    }

    private boolean pollServiceInProgress(ArmRpoExecutionDetailEntity armRpoExecutionDetail) {
        return nonNull(armRpoExecutionDetail.getArmRpoState())
            && ArmRpoHelper.inProgressRpoStatus().getId().equals(armRpoExecutionDetail.getArmRpoStatus().getId())
            && allowableInProgressStates.contains(armRpoExecutionDetail.getArmRpoState().getId());
    }

    private boolean saveBackgroundSearchCompleted(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return nonNull(armRpoExecutionDetailEntity.getArmRpoState())
            && ArmRpoHelper.saveBackgroundSearchRpoState().getId().equals(armRpoExecutionDetailEntity.getArmRpoState().getId())
            && ArmRpoHelper.completedRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId());
    }
}
