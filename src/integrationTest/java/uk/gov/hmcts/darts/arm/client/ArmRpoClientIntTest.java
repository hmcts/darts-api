package uk.gov.hmcts.darts.arm.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@TestPropertySource(properties = {
    "darts.storage.arm-api.url=http://localhost:${wiremock.server.port}"
})
class ArmRpoClientIntTest extends IntegrationBaseWithWiremock {

    private static final String GET_RECORD_MANAGEMENT_MATTER_PATH = "/api/v1/getRecordManagementMatter";

    private static final String GET_STORAGE_ACCOUNTS_PATH = "/api/v1/getStorageAccounts";

    @Autowired
    private ArmRpoClient armRpoClient;

    @Disabled("This test is failing other wiremock tests")
    @Test
    void getRecordManagementMatterShouldSucceedIfServerReturns200Success() {
        // given
        var bearerAuth = "Bearer some-token";

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
        // when
        var getRecordManagementMatterResponse = armRpoClient.getRecordManagementMatter(bearerAuth);

        // then
        verify(postRequestedFor(urlEqualTo(GET_RECORD_MANAGEMENT_MATTER_PATH))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
        );

        assertEquals("Failed to get matter id", "cb70c7fa-8972-4400-af1d-ff5dd76d2104",
                     getRecordManagementMatterResponse.getRecordManagementMatter().getMatterId());
    }


    @Disabled("This test is failing other wiremock tests but works locally")
    @Test
    void getStorageAccountsShouldSucceedIfServerReturns200Success() {
        // given
        var bearerAuth = "Bearer some-token";

        stubFor(
            WireMock.post(urlEqualTo(GET_STORAGE_ACCOUNTS_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                      {
                                          "indexes": [
                                            {
                                              "index": {
                                                "indexID": "c19454c6-c378-43c1-ae59-d0d013e30915",
                                                "isGroup": false,
                                                "name": "rm5",
                                                "displayName": "rm5",
                                                "userIndexID": "2f4d6512-64b5-4478-940d-bd29e115591c",
                                                "userID": "8b2a9527-e8e2-4430-8e51-b2af4227ff10",
                                                "azureSearchAccountID": "dac6878a-6269-48de-981d-3f2f43dfddd2",
                                                "indexStatusID": 4,
                                                "notified": false,
                                                "startDate": null,
                                                "endDate": null,
                                                "discoveryStartDate": "2023-11-16T13:53:03.94151+00:00",
                                                "discoveryEndDate": "2023-11-16T14:36:19.5597796+00:00",
                                                "buildStartDate": "2023-11-16T14:37:43.4140026+00:00",
                                                "buildEndDate": "2023-11-16T16:22:19.4438814+00:00",
                                                "resumeStartDate": "2024-07-25T10:56:39.5283925+00:00",
                                                "resumeEndDate": null,
                                                "stoppingStartDate": "2024-07-25T09:21:05.1366667+00:00",
                                                "stoppingEndDate": null,
                                                "createdDate": "2023-11-16T13:52:49.614141+00:00",
                                                "totalTime": 8883.4780311,
                                                "blobCount": 768987,
                                                "blobsProcessed": 768987,
                                                "indexDiscoveryItemsCount": 1,
                                                "indexDiscoveryItemsProcessed": 1,
                                                "exceptionsCount": null,
                                                "indexBlobExceptionsCount": 0,
                                                "indexDiscoveryItemExceptionsCount": null,
                                                "indexBatchJobPartitionsCount": 277,
                                                "indexExceptionBatchPartitionsCount": null,
                                                "indexUpdateBatchPartitionsCount": 299,
                                                "indexUpdateExceptionBatchPartitionsCount": null,
                                                "indexBatchLastJobPartitionsCount": null,
                                                "indexBlobPartitionsCount": 7651,
                                                "indexBlobJobExceptionPartitionsCount": null,
                                                "indexBlobLastJobExceptionPartitionsCount": null,
                                                "indexDiscoveryItemPartitionsCount": 1,
                                                "indexDiscoveryItemExceptionPartitionsCount": null,
                                                "indexJobExceptionPartitionsCount": null,
                                                "indexLastJobExceptionPartitionsCount": null,
                                                "tablePartitionSize": 100,
                                                "updateDate": "2023-11-16T13:52:49.614141+00:00",
                                                "lastJobID": 0,
                                                "jobID": 1,
                                                "indexBlobLastJobID": null,
                                                "indexBlobJobID": 0,
                                                "isContinous": true,
                                                "isPrimary": true,
                                                "isUsedForRM": true,
                                                "continousIndexBlobPartitionsCount": 7935,
                                                "requestSizeLimit": 100,
                                                "skipContentOverLimit": true,
                                                "skipContentIfParserError": true,
                                                "fileSizeLimitToTikaParser": 52428800,
                                                "sortByResultField": false,
                                                "blobContainer": "cloud360",
                                                "continuousIndexBatchSize": 16,
                                                "continousTablePartitionSize": 100,
                                                "isDeleted": false,
                                                "mainQueueProcessPriority": 0,
                                                "secondaryQueueProcessPriority": 1,
                                                "buildBatchesInQueue": 1350,
                                                "buildBatchesProcessed": 1350,
                                                "buildContinuationToken": "0000000000000000276-0000000000000000000",
                                                "buildExceptionContinuationToken": null,
                                                "updateContinuationToken": "0000000000000000298-0000000000000000000",
                                                "updateExceptionContinuationToken": null,
                                                "countOnly": false,
                                                "errorCodes": null,
                                                "esIndexRolloverSize": null,
                                                "esIndexRolloverSizePerShard": 40,
                                                "esIndexNoReplicas": 1,
                                                "esIndexNoShards": 6,
                                                "isDiscoveryCancelled": false,
                                                "indexContinuousLastSavedBlobExceptionPartitionsCount": null,
                                                "continuousExceptionsInProgress": 0,
                                                "preparingContinuousErrorBatches": false,
                                                "schemaUpdated": false,
                                                "discoveryItemsLock": false,
                                                "blobExceptionsStreamUpdateNeeded": false,
                                                "streamIDsToProcess": null,
                                                "indexUpdateBlobPartitionsCount": 315,
                                                "indexUpdateExceptionPartitionsCount": 176,
                                                "indexUpdateExceptionsCount": 822,
                                                "updateBlobCount": 3918,
                                                "updateBlobsProcessed": 3918,
                                                "indexLastSavedUpdateExceptionPartitionsCount": null,
                                                "updateExceptionsInProgress": 0,
                                                "preparingUpdateErrorBatches": false,
                                                "poisonHandlingFailed": false
                                              },
                                              "isMultiStream": false,
                                              "children": []
                                            }
                                          ],
                                          "itemsCount": 1,
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
        // when
        var getStorageAccountsResponse = armRpoClient.getStorageAccounts(bearerAuth, createStorageAccountRequest());

        // then
        verify(postRequestedFor(urlEqualTo(GET_STORAGE_ACCOUNTS_PATH))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
        );

        assertEquals("Failed to get storage account index name", "rm5",
                     getStorageAccountsResponse.getIndexes().getFirst().getIndex().getName());
        assertEquals("Failed to get storage account index id", "c19454c6-c378-43c1-ae59-d0d013e30915",
                     getStorageAccountsResponse.getIndexes().getFirst().getIndex().getIndexId());

    }


    private static StorageAccountRequest createStorageAccountRequest() {
        StorageAccountRequest storageAccountRequest = StorageAccountRequest.builder()
            .onlyKeyAccessType(false)
            .storageType(1)
            .build();
        return storageAccountRequest;
    }

}
