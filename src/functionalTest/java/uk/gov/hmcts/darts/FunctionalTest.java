package uk.gov.hmcts.darts;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.darts.util.TestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class FunctionalTest {

    private AccessTokenClient accessTokenClient;
    private Properties properties;

    @BeforeEach
    public void setUp() {
        configureRestAssured();
        loadProperties();
        accessTokenClient = new AccessTokenClient();
    }

    @SneakyThrows
    public String getUri(String endpoint) {
        String baseUri = TestUtils.getEnvVarValue("TEST_URL", "http://localhost:4550");
        return new URIBuilder(baseUri)
            .setPath(endpoint)
            .build()
            .toString();
    }

    public RequestSpecification buildRequestWithAuth() {
        return RestAssured.given()
            .header("Authorization", String.format("Bearer %s", getAccessToken()));
    }

    private void configureRestAssured() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @SneakyThrows
    private void loadProperties() {
        Properties properties;
        var configResource = new ClassPathResource("application-functionalTest.yaml");
        try (InputStream inputStream = new FileInputStream(configResource.getFile())) {
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Could not read properties file", e);
        }
        this.properties = properties;
    }

    private String getAccessToken() {
        return accessTokenClient.getAccessToken(
            properties.getProperty("token-uri"),
            properties.getProperty("client-id"),
            properties.getProperty("scope"),
            TestUtils.getEnvVarValue("ROPC_CLIENT_SECRET"),
            TestUtils.getEnvVarValue("ROPC_USERNAME"),
            TestUtils.getEnvVarValue("ROPC_PASSWORD")
        );
    }

}
