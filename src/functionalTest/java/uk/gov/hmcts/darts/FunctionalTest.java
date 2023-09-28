package uk.gov.hmcts.darts;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.configuration.AccessTokenClientConfiguration;
import uk.gov.hmcts.darts.configuration.AzureAdAuthenticationProperties;
import uk.gov.hmcts.darts.configuration.AzureAdB2CAuthenticationProperties;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;

@SpringBootTest(
    classes = { AccessTokenClientConfiguration.class, AzureAdAuthenticationProperties.class, AzureAdB2CAuthenticationProperties.class },
    webEnvironment = WebEnvironment.NONE
)
@ActiveProfiles({"dev", "functionalTest"})
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class FunctionalTest {

    @Autowired
    private AccessTokenClient externalAccessTokenClient;

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
            .header("Authorization", String.format("Bearer %s", externalAccessTokenClient.getAccessToken()));
    }

    private void configureRestAssured() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    public String getContentsFromFile(String filelocation) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(filelocation);
        if (resource == null) {
            throw new IOException(MessageFormat.format("File not found {0}", filelocation));
        }
        File file = new File(resource.getFile());
        return FileUtils.readFileToString(file, "UTF-8");

    }

}
