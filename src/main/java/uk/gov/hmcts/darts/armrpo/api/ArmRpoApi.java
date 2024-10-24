package uk.gov.hmcts.darts.armrpo.api;

import uk.gov.hmcts.darts.armrpo.model.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.armrpo.model.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.armrpo.model.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.armrpo.model.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.armrpo.model.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.armrpo.model.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.armrpo.model.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.armrpo.model.StorageAccountResponse;

import java.io.InputStream;
import java.util.List;

public interface ArmRpoApi {

    RecordManagementMatterResponse getRecordManagementMatter(String bearerToken, Integer executionId);

    IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, Integer executionId);

    StorageAccountResponse getStorageAccounts(String bearerToken, Integer executionId);

    ProfileEntitlementResponse getProfileEntitlements(String bearerToken, Integer executionId);

    MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(String bearerToken, Integer executionId);

    ArmAsyncSearchResponse addAsyncSearch(String bearerToken, Integer executionId);

    void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName);

    ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(String bearerToken, Integer executionId);

    boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                  List<MasterIndexFieldByRecordClassSchemaResponse> headerColumns);

    ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(String bearerToken, Integer executionId);

    List<String> getProductionOutputFiles(String bearerToken, Integer executionId);

    InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileID);

    void removeProduction(String bearerToken, Integer executionId);
}
