package uk.gov.hmcts.darts.arm.rpo;

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

public interface ArmRpoApi {

    RecordManagementMatterResponse getRecordManagementMatter(String bearerToken, Integer executionId);

    IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, Integer executionId);

    StorageAccountResponse getStorageAccounts(String bearerToken, Integer executionId);

    ProfileEntitlementResponse getProfileEntitlements(String bearerToken, Integer executionId);

    MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(String bearerToken, Integer executionId, Integer rpoStageId);

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
