package uk.gov.hmcts.darts.armrpo.armrpo.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.model.rpo.StorageAccountResponse;

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
    public MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(String bearerToken, Integer executionId, Integer rpoStageId);
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ArmAsyncSearchResponse addAsyncSearch(String bearerToken, Integer executionId) {
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
