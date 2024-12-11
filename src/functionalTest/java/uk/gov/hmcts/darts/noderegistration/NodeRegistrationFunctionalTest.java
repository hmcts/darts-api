package uk.gov.hmcts.darts.noderegistration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeRegistrationFunctionalTest extends FunctionalTest {

    public static final String POST_REGISTER_DEVICE = "/register-devices";

    @Test
    void testRegisterDevice() {
        String courthouseName = "FUNC-SWANSEA-HOUSE-" + randomAlphanumeric(7);
        String courtroomName = "FUNC-SWANSEA-ROOM-" + randomAlphanumeric(7);

        createCourtroomAndCourthouse(courthouseName, courtroomName);

        @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
        String ipAddress = "192.0.0.1";
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .queryParam("node_type", "DAR")
            .queryParam("courthouse", courthouseName)
            .queryParam("courtroom", courtroomName)
            .queryParam("host_name", "XXXXX.MMM.net")
            .queryParam("mac_address", "6A-5F-90-A4-2C-12")
            .queryParam("ip_address", ipAddress)
            .when()
            .baseUri(getUri(POST_REGISTER_DEVICE))
            .redirects().follow(false)
            .post().then().extract().response();

        assertEquals(200, response.getStatusCode());
    }

    @AfterEach
    void cleanUp() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();
    }
}
