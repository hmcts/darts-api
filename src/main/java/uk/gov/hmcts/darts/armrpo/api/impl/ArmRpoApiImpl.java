package uk.gov.hmcts.darts.armrpo.api.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.armrpo.api.ArmRpoApi;
import uk.gov.hmcts.darts.armrpo.model.ARMAsyncSearchResponse;
import uk.gov.hmcts.darts.armrpo.model.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.armrpo.model.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.armrpo.model.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.armrpo.model.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.armrpo.model.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.armrpo.model.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.armrpo.model.StorageAccountResponse;

import java.io.InputStream;
import java.util.List;

@Service
public class ArmRpoApiImpl implements ArmRpoApi {

    @Override
    public RecordManagementMatterResponse getRecordManagementMatter(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public StorageAccountResponse getStorageAccounts(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ProfileEntitlementResponse getProfileEntitlements(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ARMAsyncSearchResponse addAsyncSearch(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                         List<MasterIndexFieldByRecordClassSchemaResponse> headerColumns) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public List<String> getProductionOutputFiles(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileID) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void removeProduction(String bearerToken, Integer executionId) {
        throw new NotImplementedException("Method not implemented");
    }
}
