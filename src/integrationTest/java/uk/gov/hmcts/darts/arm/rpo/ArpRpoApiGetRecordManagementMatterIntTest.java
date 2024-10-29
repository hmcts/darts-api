package uk.gov.hmcts.darts.arm.rpo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
    "darts.storage.arm-api.url=http://localhost:${wiremock.server.port}"
})
class ArpRpoApiGetRecordManagementMatterIntTest extends IntegrationBaseWithWiremock {

    private static final String GET_RECORD_MANAGEMENT_MATTER_PATH = "/api/v1/getRecordManagementMatter";

    @Autowired
    private ArmRpoApi armRpoApi;


    @Test
    void getRecordManagementMatterShouldSucceedIfServerReturns200Success() {

        // given
        stubFor(
            WireMock.post(urlEqualTo(GET_RECORD_MANAGEMENT_MATTER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                      {
                                           "recordManagementMatter": {
                                               "matterCategory": 3,
                                               "matterID": "cb70c7fa-8972-4400-af1d-ff5dd76d2104",
                                               "name": "Records Management",
                                               "isQuickSearch": false,
                                               "isUsedForRM": true,
                                               "description": "Records Management",
                                               "createdDate": "2022-12-14T08:50:55.75+00:00",
                                               "type": 1,
                                               "status": 0,
                                               "userID": null,
                                               "backgroundJobID": null,
                                               "isClosed": false
                                           },
                                           "status": 200,
                                           "demoMode": false,
                                           "isError": false,
                                           "responseStatus": 0,
                                           "responseStatusMessages": null,
                                           "exception": null,
                                           "message": null
                                       }
                                      """
                        )
                        .withStatus(200)));

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        var getRecordManagementMatterResponse = armRpoApi.getRecordManagementMatter(bearerAuth, armRpoExecutionDetail.getId(), userAccount);

        // then
        assertNotNull(getRecordManagementMatterResponse);

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_RECORD_MANAGEMENT_MATTER.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.COMPLETED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertEquals(getRecordManagementMatterResponse.getRecordManagementMatter().getMatterId(), armRpoExecutionDetailEntityUpdated.getMatterId());

        WireMock.verify(postRequestedFor(urlPathMatching(GET_RECORD_MANAGEMENT_MATTER_PATH))
                            .withHeader("Authorization", new RegexPattern(bearerAuth)));

    }

    @Test
    void getRecordManagementMatterShouldFailIfServerReturns200SuccessWithMissingMatterId() {

        // given
        stubFor(
            WireMock.post(urlEqualTo(GET_RECORD_MANAGEMENT_MATTER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                      {
                                           "recordManagementMatter": {
                                               "matterCategory": 3,
                                               "name": "Records Management",
                                               "isQuickSearch": false,
                                               "isUsedForRM": true,
                                               "description": "Records Management",
                                               "createdDate": "2022-12-14T08:50:55.75+00:00",
                                               "type": 1,
                                               "status": 0,
                                               "userID": null,
                                               "backgroundJobID": null,
                                               "isClosed": false
                                           },
                                           "status": 200,
                                           "demoMode": false,
                                           "isError": false,
                                           "responseStatus": 0,
                                           "responseStatusMessages": null,
                                           "exception": null,
                                           "message": null
                                       }
                                      """
                        )
                        .withStatus(200)));

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when
        var getRecordManagementMatterResponse = armRpoApi.getRecordManagementMatter(bearerAuth, armRpoExecutionDetail.getId(), userAccount);

        // then
        assertNotNull(getRecordManagementMatterResponse);

        var armRpoExecutionDetailEntityUpdated = dartsPersistence.getArmRpoExecutionDetailRepository().findById(armRpoExecutionDetail.getId()).get();
        assertEquals(ArmRpoStateEnum.GET_RECORD_MANAGEMENT_MATTER.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoState().getId());
        assertEquals(ArmRpoStatusEnum.FAILED.getId(), armRpoExecutionDetailEntityUpdated.getArmRpoStatus().getId());
        assertNull(armRpoExecutionDetailEntityUpdated.getMatterId());

        WireMock.verify(postRequestedFor(urlPathMatching(GET_RECORD_MANAGEMENT_MATTER_PATH))
                            .withHeader("Authorization", new RegexPattern(bearerAuth)));

    }

    @Test
    void getRecordManagementMatterFailsWhenClientReturns400Error() {

        // given
        stubFor(
            WireMock.post(urlEqualTo(GET_RECORD_MANAGEMENT_MATTER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withStatus(400)));

        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setCreatedBy(userAccount);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccount);
        var armRpoExecutionDetail = dartsPersistence.save(armRpoExecutionDetailEntity);

        var bearerAuth = "Bearer some-token";

        // when/then
        assertThrows(FeignException.class, () -> armRpoApi.getRecordManagementMatter(bearerAuth, armRpoExecutionDetail.getId(), userAccount));

    }

}
