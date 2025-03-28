package uk.gov.hmcts.darts;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
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
import uk.gov.hmcts.darts.configuration.AzureAdB2CDarPcMidtierGlobalAuthenticationProperties;
import uk.gov.hmcts.darts.configuration.AzureAdB2CGlobalAuthenticationProperties;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@SpringBootTest(
    classes = {AccessTokenClientConfiguration.class, AzureAdAuthenticationProperties.class,
        AzureAdB2CAuthenticationProperties.class, AzureAdB2CGlobalAuthenticationProperties.class,
        AzureAdB2CDarPcMidtierGlobalAuthenticationProperties.class},
    webEnvironment = WebEnvironment.NONE
)
@ActiveProfiles({"dev", "functionalTest"})
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class FunctionalTest {

    protected static final String COURTHOUSE_SWANSEA = "FUNC-SWANSEA";

    @Autowired
    private AccessTokenClient externalAccessTokenClient;

    @Autowired
    private AccessTokenClient externalGlobalAccessTokenClient;

    @Autowired
    private AccessTokenClient internalAccessTokenClient;

    @Autowired
    private AccessTokenClient externalDarPcMidTierGlobalAccessTokenClient;

    @Value("${deployed-application-uri}")
    private URI baseUri;

    @BeforeEach
    void setUp() {
        configureRestAssured();
        enableAccessTokenCache();
    }


    @SneakyThrows
    public String getUri(String endpoint) {
        return baseUri + endpoint;
    }

    protected void enableAccessTokenCache() {
        setAccessTokenCache(true);
    }

    protected void disableAccessTokenCache() {
        setAccessTokenCache(false);
    }

    protected void setAccessTokenCache(boolean enable) {
        externalAccessTokenClient.setEnableAccessTokenCache(enable);
        externalGlobalAccessTokenClient.setEnableAccessTokenCache(enable);
        internalAccessTokenClient.setEnableAccessTokenCache(enable);
        externalDarPcMidTierGlobalAccessTokenClient.setEnableAccessTokenCache(enable);
    }


    public RequestSpecification buildRequestWithExternalAuth() {
        return buildRequestWithAuth(externalAccessTokenClient);
    }

    public RequestSpecification buildRequestWithExternalGlobalAccessAuth() {
        return buildRequestWithAuth(externalGlobalAccessTokenClient);
    }

    public RequestSpecification buildRequestWithInternalAuth() {
        return buildRequestWithAuth(internalAccessTokenClient);
    }

    public RequestSpecification buildRequestWithExternalDarMidTierGlobalAccessTokenClient() {
        return buildRequestWithAuth(externalDarPcMidTierGlobalAccessTokenClient);
    }

    public void clean() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();
    }

    protected RequestSpecification buildRequestWithAuth(AccessTokenClient accessTokenClient) {
        return RestAssured.given()
            .header("Authorization", String.format("Bearer %s", accessTokenClient.getAccessToken()));
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

    protected static String randomCaseNumber() {
        return "FUNC-CASE-" + randomAlphanumeric(7);
    }

    protected void createCourtroomAndCourthouse(String courthouseName, String courtroomName) {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/courthouse/" + courthouseName + "/courtroom/" + courtroomName))
            .redirects().follow(false)
            .post();
    }

    protected String createCaseRetentions() {
        String caseNumber = randomCaseNumber();
        Response response = buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/case-retentions/caseNumber/" + caseNumber))
            .redirects().follow(false)
            .post().then()
            .assertThat()
            .statusCode(200)
            .extract().response();
        return response.asString();
    }

    @SuppressWarnings("PMD.UselessOperationOnImmutable")
    protected boolean isIsoDateTimeString(String string) {
        try {
            LocalDateTime.parse(string, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            return false;
        }
        return true;
    }

}