package uk.gov.hmcts.darts.events;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.cloud.contract.spec.internal.HttpStatus.OK;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventMapperFunctionalTest extends FunctionalTest {

    private static final String EVENT_MAPPINGS_ENDPOINT_URL = "/admin/event-mappings";

    private static final String EVENT_HANDLERS_ENDPOINT_URL = "/admin/event-handlers";

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void getAllEventMappings() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(EVENT_MAPPINGS_ENDPOINT_URL))
            .get()
            .then()
            .assertThat()
            .statusCode(OK)
            .extract().response();

        assertNotNull(response);
    }

    @Test
    void getAllEventHandlers() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(EVENT_HANDLERS_ENDPOINT_URL))
            .get()
            .then()
            .assertThat()
            .statusCode(OK)
            .extract().response();

        assertNotNull(response);
    }

    @Test
    void getEventMappingById() {

        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(EVENT_MAPPINGS_ENDPOINT_URL + "/1"))
            .get()
            .then()
            .assertThat()
            .statusCode(OK)
            .extract().response();

        assertNotNull(response);
    }

}
