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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum.GET_EXTENDED_PRODUCTIONS_BY_MATTER;

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
    public void pollArmRpo(boolean isManualRun, Duration pollDuration, int batchSize) {
        log.info("Polling ARM RPO - isManualRun: {} poll duration: {}, batchSize: {}", isManualRun, pollDuration, batchSize);
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

            String productionName;
            boolean createExportBasedOnSearchResultsTable;
            String uniqueProductionName;
            if (skipSteps(armRpoExecutionDetailEntity)) {
                createExportBasedOnSearchResultsTable = true;
                uniqueProductionName = armRpoExecutionDetailEntity.getProductionName();
            } else {
                // step to call ARM RPO API to get the extended searches by matter
                productionName = armRpoApi.getExtendedSearchesByMatter(bearerToken, executionId, userAccount);

                uniqueProductionName = armRpoUtil.generateUniqueProductionName(productionName);

                // step to call ARM RPO API to get the master index field by record class schema
                List<MasterIndexFieldByRecordClassSchema> headerColumns = armRpoApi.getMasterIndexFieldByRecordClassSchema(
                    bearerToken, executionId,
                    ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                    userAccount);

                // step to call ARM RPO API to create export based on search results table
                createExportBasedOnSearchResultsTable = armRpoApi.createExportBasedOnSearchResultsTable(
                    bearerToken, executionId, headerColumns, uniqueProductionName, pollDuration, userAccount);
            }
            if (createExportBasedOnSearchResultsTable) {
                processProductions(bearerToken, executionId, uniqueProductionName, userAccount, armRpoExecutionDetailEntity,
                                   batchSize);
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

    boolean skipSteps(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        if (nonNull(armRpoExecutionDetailEntity.getArmRpoState())) {
            return GET_EXTENDED_PRODUCTIONS_BY_MATTER.getId().equals(armRpoExecutionDetailEntity.getArmRpoState().getId())
                && ArmRpoHelper.inProgressRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId());
        }
        return false;
    }

    private void processProductions(String bearerToken, Integer executionId, String productionName, UserAccountEntity userAccount,
                                    ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, int batchSize) throws IOException {
        // step to call ARM RPO API to get the extended productions by matter
        boolean getExtendedProductionsByMatter = armRpoApi.getExtendedProductionsByMatter(bearerToken, executionId, productionName, userAccount);
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
                armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, tempProductionFiles, batchSize);
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
        try (var inputStream = armRpoApi.downloadProduction(bearerToken, executionId, productionExportFileId, userAccount)) {
            log.info("About to save production export file to temp workspace {}", productionExportFilename);
            Path tempProductionFile = fileOperationService.saveFileToTempWorkspace(
                inputStream,
                productionExportFilename,
                armDataManagementConfiguration,
                true
            );
            tempProductionFiles.add(tempProductionFile.toFile());
        }

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
                    try {
                        Files.delete(tempProductionFile.toPath());
                        log.info("Deleted temp production file {}", tempProductionFile.getName());
                    } catch (Exception e) {
                        log.error("Failed to delete temp production file {}", tempProductionFile.getName(), e);
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
        if (isNull(armRpoExecutionDetailEntity) || isNull(armRpoExecutionDetailEntity.getArmRpoState())) {
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
        return ArmRpoHelper.failedRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId())
            && allowableFailedStates.contains(armRpoExecutionDetailEntity.getArmRpoState().getId());
    }

    private boolean pollServiceInProgress(ArmRpoExecutionDetailEntity armRpoExecutionDetail) {
        return ArmRpoHelper.inProgressRpoStatus().getId().equals(armRpoExecutionDetail.getArmRpoStatus().getId())
            && allowableInProgressStates.contains(armRpoExecutionDetail.getArmRpoState().getId());
    }

    private boolean saveBackgroundSearchCompleted(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return ArmRpoHelper.saveBackgroundSearchRpoState().getId().equals(armRpoExecutionDetailEntity.getArmRpoState().getId())
            && ArmRpoHelper.completedRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId());
    }
}
