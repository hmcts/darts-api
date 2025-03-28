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
    void shouldGetSecurityRoles() {
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
                    "role_name": "JUDICIARY",
                    "display_name": "Judiciary",
                    "display_state": true
                  },
                  {
                    "id": 2,
                    "role_name": "REQUESTER",
                    "display_name": "Requester",
                    "display_state": true
                  },
                  {
                    "id": 3,
                    "role_name": "APPROVER",
                    "display_name": "Approver",
                    "display_state": true
                  },
                  {
                    "id": 4,
                    "role_name": "TRANSCRIBER",
                    "display_name": "Transcriber",
                    "display_state": true
                  },
                  {
                    "id": 5,
                    "role_name": "TRANSLATION_QA",
                    "display_name": "Translation QA",
                    "display_state": true
                  },
                  {
                    "id": 6,
                    "role_name": "RCJ_APPEALS",
                    "display_name": "RCJ Appeals",
                    "display_state": true
                  },
                  {
                    "id": 7,
                    "role_name": "SUPER_USER",
                    "display_name": "Super User",
                    "display_state": true
                  },
                  {
                    "id": 8,
                    "role_name": "SUPER_ADMIN",
                    "display_name": "Super Admin",
                    "display_state": true
                  },
                  {
                    "id": 9,
                    "role_name": "MEDIA_ACCESSOR",
                    "display_name": "Media Accessor",
                    "display_state": false
                  },
                  {
                    "id": 10,
                    "role_name": "DARTS",
                    "display_name": "DARTS",
                    "display_state": false
                  },
                  {
                    "id": 11,
                    "role_name": "XHIBIT",
                    "display_name": "XHIBIT",
                    "display_state": false
                  },
                  {
                    "id": 12,
                    "role_name": "CPP",
                    "display_name": "CPP",
                    "display_state": false
                  },
                  {
                    "id": 13,
                    "role_name": "DAR_PC",
                    "display_name": "DAR PC",
                    "display_state": false
                  },
                  {
                    "id": 14,
                    "role_name": "MID_TIER",
                    "display_name": "Mid Tier",
                    "display_state": false
                  },
                  {
                    "id": 15,
                    "role_name": "MEDIA_IN_PERPETUITY",
                    "display_name": "Media in Perpetuity",
                    "display_state": true
                  }
                ]
                """,
            response.asString(),
            JSONCompareMode.NON_EXTENSIBLE
        );
    }

}
