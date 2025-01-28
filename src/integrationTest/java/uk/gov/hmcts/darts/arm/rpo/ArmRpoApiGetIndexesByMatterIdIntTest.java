package uk.gov.hmcts.darts.arm.rpo;


import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ArmRpoApiGetIndexesByMatterIdIntTest extends IntegrationBase {
    @MockBean
    private ArmRpoClient armRpoClient;

    @Autowired
    private ArmRpoApi armRpoApi;


    @Test
    void getIndexesByMatterIdSuccess() {

        // given
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);

        IndexesByMatterIdResponse.Index index = new IndexesByMatterIdResponse.Index();
        IndexesByMatterIdResponse.IndexDetails indexDetails = new IndexesByMatterIdResponse.IndexDetails();
        indexDetails.setIndexId("indexId");
        index.setIndex(indexDetails);
        response.setIndexes(List.of(index));

        when(armRpoClient.getIndexesByMatterId(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        armRpoApi.getIndexesByMatterId(bearerAuth, armRpoExecutionDetail.getId(), "matterId", userAccount);

        // then
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_INDEXES_BY_MATTERID.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertEquals("indexId", armRpoExecutionDetailEntityUpdated.getIndexId());

    }

    @Test
    void getIndexesByMatterIdShouldFailWithMissingMatterId() {

        // given
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        response.setStatus(200);
        response.setIsError(false);
        when(armRpoClient.getIndexesByMatterId(any(), any())).thenReturn(response);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.getIndexesByMatterId(bearerAuth, armRpoExecutionDetail.getId(), "matterId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to find any indexes by matter ID in response"));
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_INDEXES_BY_MATTERID.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getMatterId());
    }

    @Test
    void getIndexesByMatterIdFailsWhenClientReturns400Error() {

        // given
        when(armRpoClient.getIndexesByMatterId(any(), any())).thenThrow(FeignException.BadRequest.class);

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        ArmRpoException armRpoException = assertThrows(ArmRpoException.class, () ->
            armRpoApi.getIndexesByMatterId(bearerAuth, armRpoExecutionDetail.getId(), "matterId", userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO get indexes by matter ID: Unable to get ARM RPO response"));
        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).orElseThrow();
        assertEquals(ArmRpoStateEnum.GET_INDEXES_BY_MATTERID.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getMatterId());
    }

}
