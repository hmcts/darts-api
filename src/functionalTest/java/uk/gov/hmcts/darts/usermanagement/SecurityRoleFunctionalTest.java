package uk.gov.hmcts.darts.usermanagement;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityRoleFunctionalTest extends FunctionalTest {

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void shouldCreateSecurityGroup() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-roles"))
            .get()
            .thenReturn();

        assertEquals(200, response.statusCode());

        JSONAssert.assertEquals(
            """
                [
                  {
                    "id": 1,
                    "display_name": "Approver",
                    "display_state": true
                  },
                  {
                    "id": 2,
                    "display_name": "Requestor",
                    "display_state": true
                  },
                  {
                    "id": 3,
                    "display_name": "Judge",
                    "display_state": true
                  },
                  {
                    "id": 4,
                    "display_name": "Transcriber",
                    "display_state": true
                  },
                  {
                    "id": 5,
                    "display_name": "Translation QA",
                    "display_state": true
                  },
                  {
                    "id": 6,
                    "display_name": "RCJ Appeals",
                    "display_state": true
                  },
                  {
                    "id": 7,
                    "display_name": "XHIBIT",
                    "display_state": true
                  },
                  {
                    "id": 8,
                    "display_name": "CPP",
                    "display_state": true
                  },
                  {
                    "id": 9,
                    "display_name": "DAR PC",
                    "display_state": true
                  },
                  {
                    "id": 10,
                    "display_name": "Mid Tier",
                    "display_state": true
                  },
                  {
                    "id": 11,
                    "display_name": "Admin",
                    "display_state": true
                  }
                ]
                """,
            response.asString(),
            JSONCompareMode.NON_EXTENSIBLE
        );
    }

}
