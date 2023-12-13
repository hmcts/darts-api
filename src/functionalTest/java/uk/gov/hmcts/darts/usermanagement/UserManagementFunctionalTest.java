package uk.gov.hmcts.darts.usermanagement;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserManagementFunctionalTest extends FunctionalTest {

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void shouldCreateUser() {
        Response response = createUser();

        JSONAssert.assertEquals(
            """
                {
                    "id": "",
                    "full_name": "James Smith",
                    "email_address": "james.smith@hmcts.net",
                    "description": "A temporary user created by functional test",
                    "active": true,
                    "security_groups": [ ]
                }
                """,
            response.asString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("\\d+"))
            )
        );
    }

    @Test
    void shouldModifyUser() {
        Response createUserResponse = createUser();
        int userId = new JSONObject(createUserResponse.asString())
            .getInt("id");

        Response modifyUserResponse = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/users/" + userId))
            .contentType(ContentType.JSON)
            .body("""
                      {
                           "full_name": "Jimmy Smith"
                      }
                      """)
            .patch()
            .thenReturn();

        JSONAssert.assertEquals(
            """
                {
                    "id": "",
                    "full_name": "Jimmy Smith",
                    "email_address": "james.smith@hmcts.net",
                    "description": "A temporary user created by functional test",
                    "active": true,
                    "security_groups": [ ]
                }
                """,
            modifyUserResponse.asString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("^" + userId + "$"))
            )
        );
    }

    private Response createUser() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/users"))
            .contentType(ContentType.JSON)
            .body("""
                      {
                           "full_name": "James Smith",
                           "email_address": "james.smith@hmcts.net",
                           "description": "A temporary user created by functional test"
                      }
                      """)
            .post()
            .thenReturn();

        assertEquals(201, response.getStatusCode());

        return response;
    }

}
