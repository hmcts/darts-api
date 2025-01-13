package uk.gov.hmcts.darts.arm.rpo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ArmRpoApiGetExtendedSearchesByMatterIntTest extends IntegrationBase {

    public static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";
    @MockBean
    private ArmRpoClient armRpoClient;

    @MockBean
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @Autowired
    private ArmRpoApi armRpoApi;


    @Test
    void getExtendedSearchesByMatterSuccess() {
        // given
        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse = new ExtendedSearchesByMatterResponse();
        extendedSearchesByMatterResponse.setStatus(200);
        extendedSearchesByMatterResponse.setIsError(false);
        ExtendedSearchesByMatterResponse.Search search = new ExtendedSearchesByMatterResponse.Search();
        search.setTotalCount(4);
        search.setName(PRODUCTION_NAME);
        search.setIsSaved(true);
        ExtendedSearchesByMatterResponse.SearchDetail searchDetail = new ExtendedSearchesByMatterResponse.SearchDetail();
        searchDetail.setSearch(search);
        extendedSearchesByMatterResponse.setSearches(List.of(searchDetail));

        when(armRpoClient.getExtendedSearchesByMatter(any(), any())).thenReturn(extendedSearchesByMatterResponse);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setMatterId("1");
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);
        var bearerAuth = "Bearer some-token";

        // when
        String result = armRpoApi.getExtendedSearchesByMatter(bearerAuth, armRpoExecutionDetail.getId(), userAccount);

        // then
        assertEquals(PRODUCTION_NAME, result);
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_EXTENDED_SEARCHES_BY_MATTER.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertEquals(4, armRpoExecutionDetailEntityUpdated.getSearchItemCount());

    }
}
