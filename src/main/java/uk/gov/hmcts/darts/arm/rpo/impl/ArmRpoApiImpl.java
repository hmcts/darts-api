package uk.gov.hmcts.darts.arm.rpo.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.arm.rpo.AddAsyncSearchService;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.rpo.CreateExportBasedOnSearchResultsTableService;
import uk.gov.hmcts.darts.arm.rpo.DownloadProductionService;
import uk.gov.hmcts.darts.arm.rpo.GetExtendedProductionsByMatterService;
import uk.gov.hmcts.darts.arm.rpo.GetExtendedSearchesByMatterService;
import uk.gov.hmcts.darts.arm.rpo.GetIndexesByMatterIdService;
import uk.gov.hmcts.darts.arm.rpo.GetMasterIndexFieldByRecordClassSchemaService;
import uk.gov.hmcts.darts.arm.rpo.GetProductionOutputFilesService;
import uk.gov.hmcts.darts.arm.rpo.GetProfileEntitlementsService;
import uk.gov.hmcts.darts.arm.rpo.GetRecordManagementMatterService;
import uk.gov.hmcts.darts.arm.rpo.GetStorageAccountsService;
import uk.gov.hmcts.darts.arm.rpo.RemoveProductionService;
import uk.gov.hmcts.darts.arm.rpo.SaveBackgroundSearchService;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoApiImpl implements ArmRpoApi {

    private final GetRecordManagementMatterService getRecordManagementMatterService;
    private final GetIndexesByMatterIdService getIndexesByMatterIdService;
    private final GetStorageAccountsService getStorageAccountsService;
    private final GetProfileEntitlementsService getProfileEntitlementsService;
    private final GetMasterIndexFieldByRecordClassSchemaService getMasterIndexFieldByRecordClassSchemaService;
    private final AddAsyncSearchService addAsyncSearchService;
    private final SaveBackgroundSearchService saveBackgroundSearchService;
    private final GetExtendedSearchesByMatterService getExtendedSearchesByMatterService;
    private final CreateExportBasedOnSearchResultsTableService createExportBasedOnSearchResultsTableService;
    private final GetExtendedProductionsByMatterService getExtendedProductionsByMatterService;
    private final GetProductionOutputFilesService getProductionOutputFilesService;
    private final DownloadProductionService downloadProductionService;
    private final RemoveProductionService removeProductionService;

    @Override
    public void getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        getRecordManagementMatterService.getRecordManagementMatter(bearerToken, executionId, userAccount);
    }

    @Override
    public void getIndexesByMatterId(String bearerToken, Integer executionId, String matterId, UserAccountEntity userAccount) {
        getIndexesByMatterIdService.getIndexesByMatterId(bearerToken, executionId, matterId, userAccount);
    }

    @Override
    public void getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        getStorageAccountsService.getStorageAccounts(bearerToken, executionId, userAccount);
    }

    @Override
    public void getProfileEntitlements(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        getProfileEntitlementsService.getProfileEntitlements(bearerToken, executionId, userAccount);
    }

    @Override
    public List<MasterIndexFieldByRecordClassSchema> getMasterIndexFieldByRecordClassSchema(String bearerToken, Integer executionId,
                                                                                            ArmRpoStateEntity rpoStateEntity,
                                                                                            UserAccountEntity userAccount) {
        return getMasterIndexFieldByRecordClassSchemaService.getMasterIndexFieldByRecordClassSchema(bearerToken, executionId, rpoStateEntity, userAccount);
    }

    @Override
    public String addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        return addAsyncSearchService.addAsyncSearch(bearerToken, executionId, userAccount);
    }

    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount) {
        saveBackgroundSearchService.saveBackgroundSearch(bearerToken, executionId, searchName, userAccount);
    }

    @Override
    public String getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        return getExtendedSearchesByMatterService.getExtendedSearchesByMatter(bearerToken, executionId, userAccount);
    }

    @Override
    public boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                         List<MasterIndexFieldByRecordClassSchema> headerColumns, String uniqueProductionName,
                                                         Duration pollDuration, UserAccountEntity userAccount) {
        return createExportBasedOnSearchResultsTableService.createExportBasedOnSearchResultsTable(bearerToken, executionId, headerColumns, uniqueProductionName,
                                                                                                  pollDuration, userAccount);
    }

    @Override
    public boolean getExtendedProductionsByMatter(String bearerToken, Integer executionId, String productionName, UserAccountEntity userAccount) {
        return getExtendedProductionsByMatterService.getExtendedProductionsByMatter(bearerToken, executionId, productionName, userAccount);
    }

    @Override
    public List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        return getProductionOutputFilesService.getProductionOutputFiles(bearerToken, executionId, userAccount);
    }

    @Override
    public InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileId,
                                          UserAccountEntity userAccount) throws IOException {
        return downloadProductionService.downloadProduction(bearerToken, executionId, productionExportFileId, userAccount);
    }

    @Override
    public void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        removeProductionService.removeProduction(bearerToken, executionId, userAccount);
    }

}
