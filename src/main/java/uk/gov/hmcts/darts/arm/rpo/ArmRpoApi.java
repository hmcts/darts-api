package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ArmRpoApi {

    void getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    void getIndexesByMatterId(String bearerToken, Integer executionId, String matterId, UserAccountEntity userAccount);

    void getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    void getProfileEntitlements(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    List<MasterIndexFieldByRecordClassSchema> getMasterIndexFieldByRecordClassSchema(String bearerToken, Integer executionId, ArmRpoStateEntity rpoStateEntity,
                                                                                     UserAccountEntity userAccount);

    String addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount);

    void getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                  List<MasterIndexFieldByRecordClassSchema> headerColumns,
                                                  UserAccountEntity userAccount);

    void getExtendedProductionsByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount);

    InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileId, UserAccountEntity userAccount) throws IOException;

    void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount);
}
