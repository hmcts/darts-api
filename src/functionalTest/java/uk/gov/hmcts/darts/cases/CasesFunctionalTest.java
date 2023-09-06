package uk.gov.hmcts.darts.cases;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CasesFunctionalTest  extends FunctionalTest {
    @Test
    void getAllCases() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/cases"))
            .param("courthouse", "LEEDS")
            .param("courtroom", "ROOM")
            .param("date", "2023-06-14")
            .get()
            .then()
            .extract().response();

        assertEquals(200, response.statusCode());
    }

    @Test
    void getAllCourthouses() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/courthouses"))
            .get()
            .then()
            .extract().response();

        System.out.println("<=========================COURTHOUSES-HEADERS==================================>");
        System.out.println("HEADERS: " + response.getHeaders());
        System.out.println("<=========================COURTHOUSES-HEADERS==================================>");
        System.out.println("<=========================COURTHOUSES-BODY=====================================>");
        System.out.println("BODY: " + response.getBody().prettyPrint());
        System.out.println("<=========================COURTHOUSES-BODY======================================>");

        assertEquals(200, response.statusCode());
    }

    @Test
    void getCourthouse() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/courthouses/1"))
            .get()
            .then()
            .extract().response();

        assertEquals(200, response.statusCode());
    }

    @Test
    void getCourthouseBadRequest() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/courthouses/X"))
            .get()
            .then()
            .extract().response();

        assertEquals(400, response.statusCode());
    }
}
