package uk.gov.hmcts.darts.authentication;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.InternalFunctionalTest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InternalAuthenticationFunctionalTest extends InternalFunctionalTest {

    @Test
    void shouldAllowAccessWhenUnprotectedEndpointIsCalledWithoutAuth() {
        Response response = given()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/"))
            .get()
            .then()
            .extract().response();

        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenSecuredEndpointIsCalledWithoutAuth() {
        Response response = given()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/dummy-secured-endpoint"))
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        assertNotNull(response.getHeader("Location"));
    }

    @Test
    void shouldAllowAccessWhenSecuredEndpointIsCalledWithAuthThenFailInvalidEndpoint() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/dummy-secured-endpoint"))
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        assertEquals(404, response.statusCode());
    }


    @Test
    void shouldAllowAccessWhenSecuredEndpointIsCalledWithAuthAndLogout() {
        Response loginResponse = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/login-or-refresh"))
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        assertEquals(200, loginResponse.statusCode());

        Response logoutResponse = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri("/logout"))
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        assertEquals(200, logoutResponse.statusCode());

    }

}
