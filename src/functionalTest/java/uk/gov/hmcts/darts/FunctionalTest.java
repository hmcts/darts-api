package uk.gov.hmcts.darts;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfiguration;

import java.net.URI;

@SpringBootTest(
    classes = {AuthenticationConfiguration.class, AccessTokenClient.class},
    webEnvironment = WebEnvironment.NONE
)
@ActiveProfiles({"dev", "functionalTest"})
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class FunctionalTest {

    @Autowired
    private AccessTokenClient accessTokenClient;

    @Value("${deployed-application-uri}")
    private URI baseUri;

    @BeforeEach
    void setUp() {
        configureRestAssured();
    }

    @SneakyThrows
    public String getUri(String endpoint) {
        return new URIBuilder(baseUri)
            .setPath(endpoint)
            .build()
            .toString();
    }

    public RequestSpecification buildRequestWithAuth() {
        return RestAssured.given()
            .header("Authorization", String.format("Bearer %s", accessTokenClient.getAccessToken()));
    }

    private void configureRestAssured() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

}
