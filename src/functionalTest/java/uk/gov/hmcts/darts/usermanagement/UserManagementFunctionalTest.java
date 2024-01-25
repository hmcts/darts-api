package uk.gov.hmcts.darts.usermanagement;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.darts.FunctionalTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


class UserManagementFunctionalTest extends FunctionalTest {
    private static final String EMAIL_ADDRESS = "Email-Address";
    private static final String COURTHOUSE_ID = "courthouse_id";
    private static final String ADMIN_USERS = "/admin/users";

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
                    "security_group_ids": [ ]
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
            .baseUri(getUri("/admin/users/" + userId))
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
                    "security_group_ids": [ ],
                    "created_at": "",
                    "last_modified_at": ""
                }
                """,
            modifyUserResponse.asString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("^" + userId + "$")),
                new Customization("created_at", (actual, expected) -> isIsoDateTimeString(actual.toString())),
                new Customization("last_modified_at", (actual, expected) -> isIsoDateTimeString(actual.toString()))
            )
        );
    }

    @Test
    void getUserByEmail() {
        Response createUserResponse = createUser();
        int userId = new JSONObject(createUserResponse.asString())
            .getInt("id");

        Response getUserResponse = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri(ADMIN_USERS))
            .contentType(ContentType.JSON)
            .queryParam(COURTHOUSE_ID, 21)
            .header(EMAIL_ADDRESS, "james.smith@hmcts.net")
            .get()
            .thenReturn();

        JSONArray jsonArray = new JSONArray(getUserResponse.asString());
        List jsonArraySize = getUserResponse.jsonPath().getList("$");
        String firstDoc = jsonArray.getJSONObject(jsonArraySize.size() - 1).toString();

        JSONAssert.assertEquals(
            """
                    {
                        "id": "",
                        "full_name": "James Smith",
                        "email_address": "james.smith@hmcts.net",
                        "description": "A temporary user created by functional test",
                        "active": true,
                        "security_group_ids": [ ],
                        "created_at": "",
                        "last_modified_at": ""
                    }
                """,
            firstDoc,
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("^" + userId + "$")),
                new Customization("created_at", (actual, expected) -> isIsoDateTimeString(actual.toString())),
                new Customization("last_modified_at", (actual, expected) -> isIsoDateTimeString(actual.toString()))
            )
        );

    }

    @Test
    void shouldGetUserById() {
        Response createUserResponse = createUserWithSecurityGroups();
        int userId = new JSONObject(createUserResponse.asString())
            .getInt("id");

        Response getUserByIdResponse = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/users/" + userId))
            .get()
            .thenReturn();

        JSONAssert.assertEquals(
            """
                {
                    "id": "",
                    "full_name": "James Smith",
                    "email_address": "james.smith.get@hmcts.net",
                    "description": "A temporary user created by functional test",
                    "active": true,
                    "security_group_ids": [-1, -2, -3 ],
                    "created_at": "",
                    "last_modified_at": ""
                }
                """,
            getUserByIdResponse.asString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("^" + userId + "$")),
                new Customization("created_at", (actual, expected) -> isIsoDateTimeString(actual.toString())),
                new Customization("last_modified_at", (actual, expected) -> isIsoDateTimeString(actual.toString()))
            )
        );
    }

    private Response createUserWithSecurityGroups() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/users"))
            .contentType(ContentType.JSON)
            .body("""
                      {
                           "full_name": "James Smith",
                           "email_address": "james.smith.get@hmcts.net",
                           "description": "A temporary user created by functional test",
                           "security_group_ids": [-1, -2, -3]
                      }
                      """)
            .post()
            .thenReturn();

        assertEquals(201, response.getStatusCode());

        return response;
    }

    private Response createUser() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri(ADMIN_USERS))
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

    private boolean isIsoDateTimeString(String string) {
        try {
            LocalDateTime.parse(string, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            return false;
        }
        return true;
    }

    @Test
    void shouldGetSecurityGroups() throws JsonProcessingException {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups"))
            .contentType(ContentType.JSON)
            .get()
            .thenReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<SecurityGroupWithIdAndRole> securityGroupWithIdAndRoles = objectMapper.readValue(response.asString(),
                                                                                              new TypeReference<List<SecurityGroupWithIdAndRole>>(){});
        assertFalse(securityGroupWithIdAndRoles.isEmpty());
    }
}
