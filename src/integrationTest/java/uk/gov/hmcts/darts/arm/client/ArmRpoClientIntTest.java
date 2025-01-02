package uk.gov.hmcts.darts.arm.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import lombok.AllArgsConstructor;
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
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@TestPropertySource(properties = {
    "darts.storage.arm-api.url=http://localhost:${wiremock.server.port}"
})
class ArmRpoClientIntTest extends IntegrationBaseWithWiremock {

    private static final String BASE_JSON_DIRECTORY = "tests/arm/client/ArmRpoClientIntTest/";
    private static final String MOCK_RESPONSE_DIRECTORY = BASE_JSON_DIRECTORY + "mocks/";
    private static final String EXPECTED_RESPONSE_DIRECTORY = BASE_JSON_DIRECTORY + "expectedResponse/";

    private static final String URL_PREFIX = "/api/v1/";


    private static final String GET_RECORD_MANAGEMENT_MATTER_PATH = "/api/v1/getRecordManagementMatter";
    private static final String GET_STORAGE_ACCOUNTS_PATH = "/api/v1/getStorageAccounts";

    @Autowired
    private ArmRpoClient armRpoClient;


    private static Stream<Arguments> genericArmRpoClientTestArguments() {
        return Stream.of(
            Arguments.of("getRecordManagementMatter", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                return new ClientCallable(null, armRpoClient.getRecordManagementMatter(bearerAuth));
            }),
            Arguments.of("getStorageAccounts", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                StorageAccountRequest request = StorageAccountRequest.builder()
                    .onlyKeyAccessType(false)
                    .storageType(1)
                    .build();
                return new ClientCallable(request, armRpoClient.getStorageAccounts(bearerAuth, request));
            }),
            Arguments.of("getMasterIndexFieldByRecordClassSchema", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                MasterIndexFieldByRecordClassSchemaRequest request = MasterIndexFieldByRecordClassSchemaRequest.builder()
                    .recordClassCode("some-record-class-code")
                    .isForSearch(true)
                    .fieldType(1)
                    .usePaging(true)
                    .build();
                return new ClientCallable(request, armRpoClient.getMasterIndexFieldByRecordClassSchema(bearerAuth, request));
            }),
            Arguments.of("getProfileEntitlements", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                return new ClientCallable(null, armRpoClient.getProfileEntitlementResponse(bearerAuth));
            }),
            Arguments.of("addAsyncSearch", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                String request = "{\"request\": \"body\"}";
                return new ClientCallable(request, armRpoClient.addAsyncSearch(bearerAuth, request));
            }),
            Arguments.of("getIndexesByMatterId", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                IndexesByMatterIdRequest request = IndexesByMatterIdRequest.builder()
                    .matterId("matterId")
                    .build();
                return new ClientCallable(request, armRpoClient.getIndexesByMatterId(bearerAuth, request));
            }),
            Arguments.of("SaveBackgroundSearch", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                SaveBackgroundSearchRequest request = SaveBackgroundSearchRequest.builder()
                    .name("some-name")
                    .searchId("some-search-id")
                    .build();
                return new ClientCallable(request, armRpoClient.saveBackgroundSearch(bearerAuth, request));
            }),
            Arguments.of("getExtendedSearchesByMatter", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                String request = "{\"request\": \"body\"}";
                return new ClientCallable(request, armRpoClient.getExtendedSearchesByMatter(bearerAuth, request));
            }),
            Arguments.of("getProductionOutputFiles", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                ProductionOutputFilesRequest request = ProductionOutputFilesRequest.builder()
                    .productionId("some-production-id")
                    .build();
                return new ClientCallable(request, armRpoClient.getProductionOutputFiles(bearerAuth, request));
            }),
            Arguments.of("CreateExportBasedOnSearchResultsTable", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
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
                    .build();
                return new ClientCallable(request, armRpoClient.createExportBasedOnSearchResultsTable(bearerAuth, request));
            }),
            Arguments.of("removeProduction", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                RemoveProductionRequest request = RemoveProductionRequest.builder()
                    .productionId("some-production-id")
                    .deleteSearch(true)
                    .build();
                return new ClientCallable(request, armRpoClient.removeProduction(bearerAuth, request));
            }),
            Arguments.of("getExtendedProductionsByMatter", (BiFunction<ArmRpoClient, String, ClientCallable>) (armRpoClient, bearerAuth) -> {
                String request = "{\"request\": \"body\"}";
                return new ClientCallable(request, armRpoClient.getExtendedProductionsByMatter(bearerAuth, request));
            })
        );
    }


    @AllArgsConstructor
    static class ClientCallable {
        Object request;
        Object response;
    }


    @ParameterizedTest(name = "{0} should succeed when server returns 200")
    @MethodSource("genericArmRpoClientTestArguments")
    void generic_serverReturns200Success_ShouldSucceed(String suffix, BiFunction<ArmRpoClient, String, ClientCallable> callClient) throws IOException {
        stubFor(
            WireMock.post(urlEqualTo(URL_PREFIX + suffix))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody(TestUtils.getContentsFromFile(MOCK_RESPONSE_DIRECTORY + suffix + ".json"))
                        .withStatus(200)));
        String bearerAuth = "Bearer some-token";

        ClientCallable clientCallable = callClient.apply(armRpoClient, bearerAuth);

        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlEqualTo(URL_PREFIX + suffix))
            .withHeader(AUTHORIZATION, equalTo(bearerAuth))
            .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE));

        if (clientCallable.request != null) {
            requestPatternBuilder.withRequestBody(equalTo(TestUtils.writeAsString(clientCallable.request)));

        }
        verify(requestPatternBuilder);
        JSONAssert.assertEquals(TestUtils.getContentsFromFile(EXPECTED_RESPONSE_DIRECTORY + suffix + ".json"),
                                TestUtils.writeAsString(clientCallable.response),
                                JSONCompareMode.STRICT);
    }

    @Test
    void downloadProduction_serverReturns200Success_ShouldSucceed() throws IOException {
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

        feign.Response response = armRpoClient.downloadProduction(bearerAuth, "1234");

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlEqualTo(URL_PREFIX + url))
            .withHeader(AUTHORIZATION, equalTo(bearerAuth));


        verify(requestPatternBuilder);

        assertEquals(TestUtils.getContentsFromFile(EXPECTED_RESPONSE_DIRECTORY + suffix + ".csv"),
                     IOUtils.toString(response.body().asInputStream()));
    }
}
