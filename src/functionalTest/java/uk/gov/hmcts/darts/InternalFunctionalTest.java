package uk.gov.hmcts.darts;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.configuration.AccessTokenClientConfiguration;
import uk.gov.hmcts.darts.configuration.AzureAdAuthenticationProperties;
import uk.gov.hmcts.darts.configuration.AzureAdB2CAuthenticationProperties;

import java.net.URI;

@SpringBootTest(
    classes = { AccessTokenClientConfiguration.class, AzureAdAuthenticationProperties.class, AzureAdB2CAuthenticationProperties.class },
    webEnvironment = WebEnvironment.NONE
)
@ActiveProfiles({"dev", "functionalTest"})
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class InternalFunctionalTest {

    @Autowired
    private AccessTokenClient internalAccessTokenClient;

    @Value("${deployed-application-uri}")
    private URI baseUri;

    @BeforeEach
    void setUp() {
        configureRestAssured();
    }

    @SneakyThrows
    public String getUri(String endpoint) {
        return baseUri + endpoint;
    }

    public RequestSpecification buildRequestWithAuth() {
        return RestAssured.given()
            .header("Authorization", String.format("Bearer %s", internalAccessTokenClient.getAccessToken()));
    }

    private void configureRestAssured() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

}
