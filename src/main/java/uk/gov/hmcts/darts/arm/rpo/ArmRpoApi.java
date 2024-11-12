package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

public interface ArmRpoApi {

    void getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    void getIndexesByMatterId(String bearerToken, Integer executionId, String matterId, UserAccountEntity userAccount);

    void getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    void getProfileEntitlements(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    List<MasterIndexFieldByRecordClassSchema> getMasterIndexFieldByRecordClassSchema(String bearerToken, Integer executionId, ArmRpoStateEntity rpoStateEntity,
                                                                                     UserAccountEntity userAccount);

    void addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount);

    void getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                  List<MasterIndexFieldByRecordClassSchemaResponse> headerColumns,
                                                  UserAccountEntity userAccount);

    void getExtendedProductionsByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    DownloadResponseMetaData downloadProduction(String bearerToken, Integer executionId, String productionExportFileId, UserAccountEntity userAccount);

    void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount);
}
