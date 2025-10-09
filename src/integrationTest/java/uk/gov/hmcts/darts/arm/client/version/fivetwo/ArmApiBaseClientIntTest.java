package uk.gov.hmcts.darts.arm.client.version.fivetwo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@TestPropertySource(properties = {
    "darts.storage.arm-api.version5-2.api.api-base-url=http://localhost:${wiremock.server.port}"
})
@Slf4j
class ArmApiBaseClientIntTest extends IntegrationBaseWithWiremock {

    @Autowired
    private ArmApiBaseClient armApiBaseClient;

    private static final String BASE_JSON_DIRECTORY = "tests/arm/client/version/fivetwo/ArmApiBaseClientIntTest/";
    private static final String MOCK_RESPONSE_DIRECTORY = BASE_JSON_DIRECTORY + "mocks/";
    private static final String EXPECTED_RESPONSE_DIRECTORY = BASE_JSON_DIRECTORY + "expectedResponse/";

    private static final String URL_PREFIX = "/v1/";

    private static final String EXTERNAL_RECORD_ID = "7683ee65-c7a7-7343-be80-018b8ac13602";
    private static final String EXTERNAL_FILE_ID = "075987ea-b34d-49c7-b8db-439bfbe2496c";
    private static final String CABINET_ID = "100";
    private static final String UPDATE_METADATA_PATH = "/v3/UpdateMetadata";
    private static final String DOWNLOAD_ARM_DATA_PATH = "/v1/downloadBlob/\\S+/\\S+/\\S+";

    private static Stream<Arguments> armApiBaseClientTestArguments() {
        return Stream.of(
            Arguments.of("getRecordManagementMatter",
                         (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                             EmptyRpoRequest request = EmptyRpoRequest.builder().build();
                             return new ClientCallable(request, armApiBaseClient.getRecordManagementMatter(bearerAuth, request));
                         }),
            Arguments.of("getStorageAccounts", (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                StorageAccountRequest request = StorageAccountRequest.builder()
                    .onlyKeyAccessType(false)
                    .storageType(1)
                    .build();
                return new ClientCallable(request, armApiBaseClient.getStorageAccounts(bearerAuth, request));
            }),
            Arguments.of("getMasterIndexFieldByRecordClassSchema",
                         (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                             MasterIndexFieldByRecordClassSchemaRequest request = MasterIndexFieldByRecordClassSchemaRequest.builder()
                                 .recordClassCode("some-record-class-code")
                                 .isForSearch(true)
                                 .fieldType(1)
                                 .usePaging(true)
                                 .build();
                             return new ClientCallable(request, armApiBaseClient.getMasterIndexFieldByRecordClassSchema(bearerAuth, request));
                         }),
            Arguments.of("getProfileEntitlements", (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
                return new ClientCallable(null, armApiBaseClient.getProfileEntitlementResponse(bearerAuth, emptyRpoRequest));
            }),
            Arguments.of("addAsyncSearchRM", (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                String request = "{\"request\": \"body\"}";
                return new ClientCallable(request, armApiBaseClient.addAsyncSearch(bearerAuth, request));
            }),
            Arguments.of("getIndexesByMatterId", (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                IndexesByMatterIdRequest request = IndexesByMatterIdRequest.builder()
                    .matterId("matterId")
                    .build();
                return new ClientCallable(request, armApiBaseClient.getIndexesByMatterId(bearerAuth, request));
            }),
            Arguments.of("SaveBackgroundSearch", (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                SaveBackgroundSearchRequest request = SaveBackgroundSearchRequest.builder()
                    .name("some-name")
                    .searchId("some-search-id")
                    .build();
                return new ClientCallable(request, armApiBaseClient.saveBackgroundSearch(bearerAuth, request));
            }),
            Arguments.of("getExtendedSearchesByMatter",
                         (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                             String request = "{\"request\": \"body\"}";
                             return new ClientCallable(request, armApiBaseClient.getExtendedSearchesByMatter(bearerAuth, request));
                         }),
            Arguments.of("getProductionOutputFiles", (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                ProductionOutputFilesRequest request = ProductionOutputFilesRequest.builder()
                    .productionId("some-production-id")
                    .build();
                return new ClientCallable(request, armApiBaseClient.getProductionOutputFiles(bearerAuth, request));
            }),
            Arguments.of("CreateExportBasedOnSearchResultsTable",
                         (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                             CreateExportBasedOnSearchResultsTableRequest request = CreateExportBasedOnSearchResultsTableRequest.builder()
                                 .core("some-core")
                                 .formFields("some-form-fields")
                                 .searchId("some-search-id")
                                 .searchitemsCount(1)
                                 .headerColumns(
                                     List.of(CreateExportBasedOnSearchResultsTableRequest.HeaderColumn.builder()
                                                 .masterIndexField("some-master-index-field")
                                                 .displayName("some-display-name")
                                                 .propertyName("some-property-name")
                                                 .propertyType("some-property-type")
                                                 .isMasked(true)
                                                 .build())
                                 )
                                 .productionName("some-production-name")
                                 .storageAccountId("some-storage-account-id")
                                 .onlyForCurrentUser(Boolean.FALSE)
                                 .exportType(32)
                                 .build();
                             return new ClientCallable(request, armApiBaseClient.createExportBasedOnSearchResultsTable(bearerAuth, request));
                         }),
            Arguments.of("removeProduction", (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                RemoveProductionRequest request = RemoveProductionRequest.builder()
                    .productionId("some-production-id")
                    .deleteSearch(true)
                    .build();
                return new ClientCallable(request, armApiBaseClient.removeProduction(bearerAuth, request));
            }),
            Arguments.of("getExtendedProductionsByMatter",
                         (BiFunction<ArmApiBaseClient, String, ClientCallable>) (armApiBaseClient, bearerAuth) -> {
                             String request = "{\"request\": \"body\"}";
                             return new ClientCallable(request, armApiBaseClient.getExtendedProductionsByMatter(bearerAuth, request));
                         })
        );
    }

    @AllArgsConstructor
    static class ClientCallable {
        Object request;
        Object response;
    }

    @ParameterizedTest(name = "{0} should succeed when server returns 200")
    @MethodSource("armApiBaseClientTestArguments")
    void armApiBaseClient_Returns200Success_ShouldSucceed(String suffix,
                                                          BiFunction<ArmApiBaseClient, String, ClientCallable> callClient) throws IOException {
        String requestFileLocation = MOCK_RESPONSE_DIRECTORY + suffix + ".json";
        log.info("Testing: {} with request: {}", suffix, requestFileLocation);
        stubFor(
            WireMock.post(urlEqualTo(URL_PREFIX + suffix))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody(TestUtils.getContentsFromFile(requestFileLocation))
                        .withStatus(200)));
        String bearerAuth = "Bearer some-token";

        ClientCallable clientCallable = callClient.apply(armApiBaseClient, bearerAuth);

        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlEqualTo(URL_PREFIX + suffix))
            .withHeader(AUTHORIZATION, equalTo(bearerAuth))
            .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE));

        if (clientCallable.request != null) {
            requestPatternBuilder.withRequestBody(equalTo(TestUtils.writeAsString(clientCallable.request)));
        }
        verify(requestPatternBuilder);
        String resultFilelocation = EXPECTED_RESPONSE_DIRECTORY + suffix + ".json";
        log.info("Verifying: {} with result: {}", suffix, resultFilelocation);
        JSONAssert.assertEquals(TestUtils.getContentsFromFile(resultFilelocation),
                                TestUtils.writeAsString(clientCallable.response),
                                JSONCompareMode.STRICT);
    }

    @Test
    void downloadProduction_Returns200Success_ShouldSucceed() throws IOException {
        String url = "downloadProduction/1234/false";
        String suffix = "downloadProduction";
        stubFor(
            WireMock.get(urlEqualTo(URL_PREFIX + url))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                        .withBody(TestUtils.getContentsFromFile(MOCK_RESPONSE_DIRECTORY + suffix + ".csv"))
                        .withStatus(200)));
        String bearerAuth = "Bearer some-token";

        try (feign.Response response = armApiBaseClient.downloadProduction(bearerAuth, "1234")) {
            RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlEqualTo(URL_PREFIX + url))
                .withHeader(AUTHORIZATION, equalTo(bearerAuth));

            verify(requestPatternBuilder);

            assertEquals(TestUtils.getContentsFromFile(EXPECTED_RESPONSE_DIRECTORY + suffix + ".csv"),
                         IOUtils.toString(response.body().asInputStream()));
        }
    }

    @Test
    void getRecordManagementMatter_ShouldSucceed_IfServerReturns200Success_WithEmptyRequest() throws Exception {
        // Given
        var bearerAuth = "Bearer some-token";
        EmptyRpoRequest request = EmptyRpoRequest.builder().build();

        stubFor(
            WireMock.post(urlEqualTo("/v1/getRecordManagementMatter"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody(TestUtils.getContentsFromFile(MOCK_RESPONSE_DIRECTORY + "getRecordManagementMatter.json"))
                        .withStatus(200)));

        // When
        armApiBaseClient.getRecordManagementMatter(bearerAuth, request);

        // Then
        verify(postRequestedFor(urlEqualTo("/v1/getRecordManagementMatter"))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withRequestBody(equalTo("{}"))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
        );
    }

    @Test
    void createExportBasedOnSearchResultsTable_ShouldSucceed_WithFullRequest() throws Exception {
        // Given
        var bearerAuth = "Bearer some-token";
        CreateExportBasedOnSearchResultsTableRequest request = CreateExportBasedOnSearchResultsTableRequest.builder()
            .core("some-core")
            .formFields("some-form-fields")
            .searchId("some-search-id")
            .searchitemsCount(1)
            .headerColumns(
                List.of(CreateExportBasedOnSearchResultsTableRequest.HeaderColumn.builder()
                            .masterIndexField("some-master-index-field")
                            .displayName("some-display-name")
                            .propertyName("some-property-name")
                            .propertyType("some-property-type")
                            .isMasked(true)
                            .build())
            )
            .productionName("some-production-name")
            .storageAccountId("some-storage-account-id")
            .onlyForCurrentUser(Boolean.FALSE)
            .exportType(32)
            .build();

        stubFor(
            WireMock.post(urlEqualTo("/v1/CreateExportBasedOnSearchResultsTable"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody(TestUtils.getContentsFromFile(MOCK_RESPONSE_DIRECTORY + "CreateExportBasedOnSearchResultsTable.json"))
                        .withStatus(200)));

        // When
        armApiBaseClient.createExportBasedOnSearchResultsTable(bearerAuth, request);

        // Then
        verify(postRequestedFor(urlEqualTo("/v1/CreateExportBasedOnSearchResultsTable"))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withRequestBody(matchingJsonPath("$.productionName", equalTo("some-production-name")))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
        );
    }

    @Test
    void updateMetadata_ShouldSucceed_WhenServerReturns200Success() {
        // Given
        var bearerAuth = "Bearer some-token";
        var externalRecordId = "7683ee65-c7a7-7343-be80-018b8ac13602";
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T11:29:56.101701Z").plusYears(7);

        stubFor(
            WireMock.post(urlEqualTo(UPDATE_METADATA_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                      {
                                          "itemId": "7683ee65-c7a7-7343-be80-018b8ac13602",
                                          "cabinetId": 101,
                                          "objectId": "4bfe4fc7-4e2f-4086-8a0e-146cc4556260",
                                          "objectType": 1,
                                          "fileName": "UpdateMetadata-20241801-122819.json",
                                          "isError": false,
                                          "responseStatus": 0,
                                          "responseStatusMessages": null
                                      }
                                      """
                        )
                        .withStatus(200)));

        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(formatDateTime(eventTimestamp))
                          .build())
            .useGuidsForFields(false)
            .build();

        // When
        UpdateMetadataResponse updateMetadataResponse = armApiBaseClient.updateMetadata(bearerAuth, updateMetadataRequest);

        // Then
        verify(postRequestedFor(urlEqualTo(UPDATE_METADATA_PATH))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                   .withRequestBody(
                       matchingJsonPath("$.UseGuidsForFields", equalTo("false"))
                           .and(matchingJsonPath("$.manifest.event_date", equalTo(formatDateTime(eventTimestamp))))
                           .and(matchingJsonPath("$.itemId", equalTo(externalRecordId)))
                   ));

        assertEquals(UUID.fromString(externalRecordId), updateMetadataResponse.getItemId());
    }

    @Test
    void updateMetadata_ShouldSucceed_WithZeroTimes() {
        // Given
        var bearerAuth = "Bearer some-token";
        var externalRecordId = "7683ee65-c7a7-7343-be80-018b8ac13602";
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T00:00:00.00000Z").plusYears(7);

        stubFor(
            WireMock.post(urlEqualTo(UPDATE_METADATA_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                      {
                                          "itemId": "7683ee65-c7a7-7343-be80-018b8ac13602",
                                          "cabinetId": 101,
                                          "objectId": "4bfe4fc7-4e2f-4086-8a0e-146cc4556260",
                                          "objectType": 1,
                                          "fileName": "UpdateMetadata-20241801-122819.json",
                                          "isError": false,
                                          "responseStatus": 0,
                                          "responseStatusMessages": null
                                      }
                                      """
                        )
                        .withStatus(200)));

        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(formatDateTime(eventTimestamp))
                          .build())
            .useGuidsForFields(false)
            .build();

        // When
        UpdateMetadataResponse updateMetadataResponse = armApiBaseClient.updateMetadata(bearerAuth, updateMetadataRequest);

        // Then
        verify(postRequestedFor(urlEqualTo(UPDATE_METADATA_PATH))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                   .withRequestBody(
                       matchingJsonPath("$.UseGuidsForFields", equalTo("false"))
                           .and(matchingJsonPath("$.manifest.event_date", equalTo(formatDateTime(eventTimestamp))))
                           .and(matchingJsonPath("$.itemId", equalTo(externalRecordId)))
                   ));

        assertEquals(UUID.fromString(externalRecordId), updateMetadataResponse.getItemId());
    }

    @Test
    @SneakyThrows
    void downloadArmData_ShouldSucceed_WhenServerReturns200Success() {
        // Given
        stubFor(
            WireMock.get(urlPathMatching(DOWNLOAD_ARM_DATA_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                        .withBodyFile("testAudio.mp3")
                        .withStatus(200)));

        // When
        try (feign.Response response = armApiBaseClient.downloadArmData("Bearer token", CABINET_ID, EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID)) {

            //Then
            try (InputStream expectedInputStream = Files.newInputStream(Paths.get("src/integrationTest/resources/wiremock/__files/testAudio.mp3"))) {
                assertTrue(IOUtils.contentEquals(response.body().asInputStream(), expectedInputStream));
            }
        }
    }

    private String formatDateTime(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        return offsetDateTime.format(dateTimeFormatter);
    }
}
