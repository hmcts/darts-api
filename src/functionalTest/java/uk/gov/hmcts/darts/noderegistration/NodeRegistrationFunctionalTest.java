package uk.gov.hmcts.darts.noderegistration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.aspectj.lang.annotation.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeRegistrationFunctionalTest extends FunctionalTest {

    public static final String POST_REGISTER_DEVICE = "/register-devices";

    @Test
    void testRegisterDevice() {
        //create courtroom and courthouse
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/courthouse/func-liverpool/courtroom/1"))
            .redirects().follow(false)
            .post();

        @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
        String ipAddress = "192.0.0.1";
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .queryParam("node_type", "DAR")
            .queryParam("courthouse", "func-liverpool")
            .queryParam("court_room", "1")
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
