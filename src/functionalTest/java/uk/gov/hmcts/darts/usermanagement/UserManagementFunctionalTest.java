package uk.gov.hmcts.darts.usermanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.testutil.TestUtils;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UserManagementFunctionalTest extends FunctionalTest {

    private static final String ADMIN_USERS_BASE_PATH = "/admin/users";
    private static final String ID_PATH_PARAM_NAME = "id";
    private static final String ADMIN_USERS_BY_ID_PATH = "/admin/users/{" + ID_PATH_PARAM_NAME + "}";
    private static final String ADMIN_USERS_SEARCH_PATH = "/admin/users/search";
    private static final String EMAIL_ADDRESS_HEADER_NAME = "Email-Address";

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void shouldCreateUser() {
        Response response = createUser();

        assertEquals(201, response.getStatusCode());
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
        int userId = extractId(createUser());

        Response modifyUserResponse = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .pathParam(ID_PATH_PARAM_NAME, userId)
            .body("""
                      {
                           "full_name": "Jimmy Smith"
                      }
                      """)
            .patch(getUri(ADMIN_USERS_BY_ID_PATH))
            .thenReturn();

        assertEquals(200, modifyUserResponse.getStatusCode());
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
        int userId = extractId(createUser());

        Response getUserResponse = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .header(EMAIL_ADDRESS_HEADER_NAME, "james.smith@hmcts.net")
            .get(getUri(ADMIN_USERS_BASE_PATH))
            .thenReturn();

        JSONArray jsonArray = new JSONArray(getUserResponse.asString());
        List jsonArraySize = getUserResponse.jsonPath().getList("$");
        String firstDoc = jsonArray.getJSONObject(jsonArraySize.size() - 1).toString();

        assertEquals(200, getUserResponse.getStatusCode());
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
        int userId = extractId(createUserWithSecurityGroups());

        Response getUserByIdResponse = buildRequestWithExternalGlobalAccessAuth()
            .pathParam(ID_PATH_PARAM_NAME, userId)
            .get(getUri(ADMIN_USERS_BY_ID_PATH))
            .thenReturn();

        assertEquals(200, getUserByIdResponse.getStatusCode());
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

    @Test
    void shouldGetUsers() throws JsonProcessingException {
        // Given
        int userId = extractId(createUser());

        // When
        Response getUsersResponse = buildRequestWithExternalGlobalAccessAuth()
            .get(getUri(ADMIN_USERS_BASE_PATH))
            .thenReturn();

        // Then
        assertEquals(200, getUsersResponse.getStatusCode());
        List<UserWithIdAndTimestamps> users = TestUtils.createObjectMapper()
            .readValue(getUsersResponse.asString(), new TypeReference<>() {
            });
        assertFalse(users.isEmpty());

        Optional<UserWithIdAndTimestamps> expectedUser = users.stream()
            .filter(user -> user.getId().equals(userId))
            .findFirst();
        assertTrue(expectedUser.isPresent());
        assertNotNull(expectedUser.get().getId());
    }

    @Test
    void shouldGetUsersBySearchCriteria() throws JsonProcessingException {
        // Given
        int userId = extractId(createUser());

        // When
        Response searchResponse = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body("""
                      {
                           "email_address": "james.smith"
                      }
                      """)
            .post(getUri(ADMIN_USERS_SEARCH_PATH))
            .thenReturn();

        // Then
        assertEquals(200, searchResponse.getStatusCode());
        List<UserWithIdAndTimestamps> users = TestUtils.createObjectMapper()
            .readValue(searchResponse.asString(), new TypeReference<>() {
            });
        assertEquals(1, users.size());

        var user = users.getFirst();
        assertEquals(userId, user.getId());
    }

    private Response createUser() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body("""
                      {
                           "full_name": "James Smith",
                           "email_address": "james.smith@hmcts.net",
                           "description": "A temporary user created by functional test"
                      }
                      """)
            .post(getUri(ADMIN_USERS_BASE_PATH))
            .thenReturn();

        assertEquals(201, response.getStatusCode());

        return response;
    }

    private Response createUserWithSecurityGroups() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body("""
                      {
                           "full_name": "James Smith",
                           "email_address": "james.smith.get@hmcts.net",
                           "description": "A temporary user created by functional test",
                           "security_group_ids": [-1, -2, -3]
                      }
                      """)
            .post(getUri(ADMIN_USERS_BASE_PATH))
            .thenReturn();

        assertEquals(201, response.getStatusCode());

        return response;
    }

    private int extractId(Response response) {
        return new JSONObject(response.asString())
            .getInt("id");
    }

}
