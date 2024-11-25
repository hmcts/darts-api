package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmRpoPollService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoPollServiceImpl implements ArmRpoPollService {

    private final ArmRpoApi armRpoApi;
    private final ArmRpoHelper armRpoHelper;
    private final ArmRpoService armRpoService;
    private final UserIdentity userIdentity;

    @Override
    public void pollArmRpo(Integer batchSize) {
        /*
        Call armRpoApi.getExtendedSearchesByMatter (DMP-3806)
Call armRpoApi.getMasterIndexFieldByRecordClassSchema (DMP-4136) - Pass ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState()
returns Collection of MetadataObject
Call armRpoApi.createExportBasedOnSearchResultsTable (DMP-4138) by passing the collection of MetadataObject retrieved from previous call in headerColumn field
If return TRUE, continue to the next step
If return false, then DO NOTHING and return.
Call armRpoApi.getExtendedProductionsByMatter (DMP-4139)
Call armRpoApi.getProductionOutputFiles which will return a collection of productionExportFileID
For each productionExportFileID
if is_mock_arm_rpo_download_csv is TRUE
Fetch rpo_csv_start_hour & rpo_csv_end_hour from arm_automated_task table for ProcessE2EARMRPOPendingAutomatedTasks job.
Fetch the records from EOD with status as ARM_RPO_PENDING and data_ingestion_ts between created_ts minus rpo_csv_start_hour and
created_ts minus rpo_csv_end_hour
Inject only 10 EODs in stub header and call the stub - downloadProduction (DMP-4078)
stub will return CSV with EODs
if is_mock_arm_rpo_download_csv is FALSE
call downloadProduction (DMP-4141)
Save the CSV in blob storage (DMP-3617)
Once CSV is successfully saved, then remove the Search results by calling armRpoApi.removeProduction(DMP-4142)
Reconcile the CSV against EOD (DMP-3619)
Remove the CSV(s) from Blob storage once reconciliation is completed.
         */
        try {
            var bearerToken = "";
            var executionId = 0;
            var userAccount = userIdentity.getUserAccount();

            armRpoApi.getExtendedSearchesByMatter(bearerToken, executionId, userAccount);
            List<MasterIndexFieldByRecordClassSchema> headerColumns = armRpoApi.getMasterIndexFieldByRecordClassSchema(
                bearerToken, executionId,
                armRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState(),
                userAccount);

            var createExportBasedOnSearchResultsTable = armRpoApi.createExportBasedOnSearchResultsTable(bearerToken, executionId, headerColumns,
                                                                                                        userAccount);
            if (createExportBasedOnSearchResultsTable) {
                armRpoApi.getExtendedProductionsByMatter(bearerToken, executionId, userAccount);

                var productionOutputFiles = armRpoApi.getProductionOutputFiles(bearerToken, executionId, production.getProductionExportFileID(),
                                                                               userAccount);
                for (var productionOutputFile : productionOutputFiles) {
                    if (productionOutputFile.isMockArmRpoDownloadCsv()) {
                        var rpoCsvStartHour = 0;
                        var rpoCsvEndHour = 0;
                        var eods = armRpoService.getEodsForCsvDownload(rpoCsvStartHour, rpoCsvEndHour);
                        armRpoApi.downloadProduction(bearerToken, executionId, eods, userAccount);
                    } else {
                        var downloadProduction = armRpoApi.downloadProduction(bearerToken, executionId, userAccount);
                        if (downloadProduction) {
                            armRpoService.saveCsvInBlobStorage();
                            armRpoApi.removeProduction(bearerToken, executionId, userAccount);
                            armRpoService.reconcileCsvAgainstEod();
                            armRpoService.removeCsvFromBlobStorage();
                        }
                    }
                }

            } else {
                log.warn("createExportBasedOnSearchResultsTable returned false");
            }

        } catch (Exception e) {
            log.error("Error while polling ARM RPO", e);
        }
    }
}
