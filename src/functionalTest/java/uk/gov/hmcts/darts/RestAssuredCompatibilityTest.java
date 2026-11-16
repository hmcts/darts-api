package uk.gov.hmcts.darts;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class RestAssuredCompatibilityTest {

    private final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

    @BeforeEach
    void startWireMock() {
        wireMockServer.start();
    }

    @AfterEach
    void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void given_nestedJackson2Model_when_roundTrippingWithRestAssured_then_preserveWireContract() {
        String payload = """
            {"UseGuidsForFields":false,"itemId":"item-123",
             "manifest":{"event_date":"2026-11-15T12:34:56Z","ret_conf_score":0,"ret_conf_reason":"Confirmed"}}
            """;
        UpdateMetadataRequest request = UpdateMetadataRequest.builder()
            .useGuidsForFields(false)
            .itemId("item-123")
            .manifest(UpdateMetadataRequest.Manifest.builder()
                .eventDate("2026-11-15T12:34:56Z")
                .retConfScore(0)
                .retConfReason("Confirmed")
                .build())
            .build();
        wireMockServer.stubFor(post(urlEqualTo("/metadata"))
            .withRequestBody(equalToJson(payload))
            .willReturn(okJson(payload)));

        UpdateMetadataRequest response = given()
            .config(FunctionalTest.jackson2RestAssuredConfig())
            .baseUri(wireMockServer.baseUrl())
            .contentType(ContentType.JSON)
            .body(request)
            .post("/metadata")
            .then().statusCode(200)
            .body("manifest.ret_conf_score", equalTo(0))
            .body("UseGuidsForFields", equalTo(false))
            .extract().as(UpdateMetadataRequest.class);

        assertThat(response).isEqualTo(request);
    }
}
