package uk.gov.hmcts.darts.audit;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class AuditSearchFunctionalTest extends FunctionalTest {
    private static final String SEARCH_ENDPOINT = "/audit/search";


    @AfterEach
    void cleanData() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();
    }

    @Test
    @Order(1)
    void searchForRequestAudioAuditUsingDateRange() {

        Response response;
        response = buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/courthouse/func-swansea/courtroom/1"))
            .redirects().follow(false)
            .post();


        log.info(response.asPrettyString());

        response = buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/audit/REQUEST_AUDIO/courthouse/func-swansea"))
            .redirects().follow(false)
            .post();

        log.info(response.asPrettyString());

        response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .queryParam("from_date", OffsetDateTime.now().minusHours(1).toString())
            .queryParam("to_date", OffsetDateTime.now().plusHours(1).toString())
            .when()
            .baseUri(getUri(SEARCH_ENDPOINT))
            .redirects().follow(false)
            .get().then().extract().response();

        log.info(response.asPrettyString());

        assertEquals(200, response.statusCode());
    }

    @Test
    @Order(2)
    void searchForNonExistingAudit() {

        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .queryParam("from_date", OffsetDateTime.now().minusHours(1).toString())
            .queryParam("to_date", OffsetDateTime.now().plusHours(1).toString())
            .when()
            .baseUri(getUri(SEARCH_ENDPOINT))
            .redirects().follow(false)
            .get().then().extract().response();

        log.info(response.asPrettyString());

        assertEquals("""
                         [
                            \s
                         ]""", response.asPrettyString());
        assertEquals(200, response.statusCode());
    }


}
