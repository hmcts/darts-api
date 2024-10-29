package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.InputStream;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoApiImpl implements ArmRpoApi {

    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;

    @Override
    public RecordManagementMatterResponse getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        RecordManagementMatterResponse recordManagementMatterResponse;
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getRecordManagementMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);
        try {

            recordManagementMatterResponse = armRpoClient.getRecordManagementMatter(bearerToken);
            if (recordManagementMatterResponse != null
                && recordManagementMatterResponse.getRecordManagementMatter() != null
                && recordManagementMatterResponse.getRecordManagementMatter().getMatterId() != null) {

                armRpoExecutionDetailEntity.setMatterId(recordManagementMatterResponse.getRecordManagementMatter().getMatterId());
                armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);

            } else {
                armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccount);
            }
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error("Error during ARM get record management matter: {}", e.contentUTF8());
            armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccount);
            throw e;
        }
        return recordManagementMatterResponse;
    }

    @Override
    public IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public StorageAccountResponse getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ProfileEntitlementResponse getProfileEntitlements(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(String bearerToken,
                                                                                              Integer executionId,
                                                                                              Integer rpoStageId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ArmAsyncSearchResponse addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                         List<MasterIndexFieldByRecordClassSchemaResponse> headerColumns, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileID, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }
}
