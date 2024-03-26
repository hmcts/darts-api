package uk.gov.hmcts.darts.usermanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UserManagementFunctionalTest extends FunctionalTest {
    private static final String EMAIL_ADDRESS = "Email-Address";
    private static final String COURTHOUSE_ID = "courthouse_id";
    private static final String ADMIN_USERS = "/admin/users";
    static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void beforeAll() {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

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

    @Test
    void shouldGetSecurityGroups() throws JsonProcessingException {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups"))
            .contentType(ContentType.JSON)
            .get()
            .thenReturn();

        List<SecurityGroupWithIdAndRole> securityGroupWithIdAndRoles = MAPPER.readValue(response.asString(),
                                                                                              new TypeReference<List<SecurityGroupWithIdAndRole>>(){});
        assertFalse(securityGroupWithIdAndRoles.isEmpty());
    }

    @Test
    void shouldPatchSecurityGroups() throws JsonProcessingException {

        String postContent = """
                         {
                           "name": "<func-a-security-group>",
                           "display_name": "<A security group>",
                           "description": "func-test group"
                         }
                           """;
        postContent = postContent.replace("<func-a-security-group>", "func-a-security-group " + UUID.randomUUID());
        postContent = postContent.replace("<A security group>", "A security group " + UUID.randomUUID());

        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups"))
            .contentType(ContentType.JSON)
            .body(postContent)
            .post()
            .thenReturn();

        assertEquals(201, response.getStatusCode());

        String patchContent = """
                         {
                           "name": "<func-a-security-group-new-name>",
                           "display_name": "<A security group new name>",
                           "description": "func-test group new description",
                           "courthouse_ids": [<id1>,<id2>],
                           "user_ids": [-1,-2]
                         }
                           """;
        String newName = "func-a-security-group-new-name " + UUID.randomUUID();
        patchContent = patchContent.replace("<func-a-security-group-new-name>", newName);
        String newDisplayName = "A security group new name " + UUID.randomUUID();
        patchContent = patchContent.replace("<A security group new name>", newDisplayName);
        Integer id1 = createCourthouse("func-a_courthouse " + UUID.randomUUID(), "func-a_courthouse" + UUID.randomUUID());
        Integer id2 = createCourthouse("func-a_courthouse " + UUID.randomUUID(), "func-a_courthouse" + UUID.randomUUID());
        patchContent = patchContent.replace("<id1>", id1.toString());
        patchContent = patchContent.replace("<id2>", id2.toString());

        SecurityGroupWithIdAndRole securityGroupWithIdAndRoles = MAPPER.readValue(response.asString(),
                                                                                  new TypeReference<SecurityGroupWithIdAndRole>(){});

        response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups/" + securityGroupWithIdAndRoles.getId()))
            .contentType(ContentType.JSON)
            .body(patchContent)
            .patch()
            .thenReturn();

        SecurityGroupWithIdAndRoleAndUsers securityGroupWithIdAndRoleAndUsers = MAPPER.readValue(response.asString(),
            new TypeReference<SecurityGroupWithIdAndRoleAndUsers>(){});

        assertEquals(newName, securityGroupWithIdAndRoleAndUsers.getName());
        assertEquals(newDisplayName, securityGroupWithIdAndRoleAndUsers.getDisplayName());
        assertEquals("func-test group new description", securityGroupWithIdAndRoleAndUsers.getDescription());
        assertTrue(securityGroupWithIdAndRoleAndUsers.getCourthouseIds().contains(id1));
        assertTrue(securityGroupWithIdAndRoleAndUsers.getCourthouseIds().contains(id2));
        assertTrue(securityGroupWithIdAndRoleAndUsers.getUserIds().contains(-1));
        assertTrue(securityGroupWithIdAndRoleAndUsers.getUserIds().contains(-2));

        response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups/" + securityGroupWithIdAndRoles.getId()))
            .contentType(ContentType.JSON)
            .get()
            .thenReturn();

        SecurityGroupWithIdAndRole retrievedSecurityGroupWithIdAndRoles = MAPPER.readValue(response.asString(),
                                                                                              new TypeReference<SecurityGroupWithIdAndRole>(){});

        assertEquals(newName, retrievedSecurityGroupWithIdAndRoles.getName());
        assertEquals(newDisplayName, retrievedSecurityGroupWithIdAndRoles.getDisplayName());
        assertEquals("func-test group new description", retrievedSecurityGroupWithIdAndRoles.getDescription());
        assertTrue(retrievedSecurityGroupWithIdAndRoles.getCourthouseIds().contains(id1));
        assertTrue(retrievedSecurityGroupWithIdAndRoles.getCourthouseIds().contains(id2));
        assertTrue(retrievedSecurityGroupWithIdAndRoles.getUserIds().contains(-1));
        assertTrue(retrievedSecurityGroupWithIdAndRoles.getUserIds().contains(-2));


    }

    private Integer createCourthouse(String name, String displayName) throws JsonProcessingException {
        String content = String.format("{\"courthouse_name\": \"%s\", \"display_name\": \"%s\"}", name, displayName);
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/courthouses"))
            .contentType(ContentType.JSON)
            .body(content)
            .post()
            .thenReturn();

        ExtendedCourthousePost extendedCourthousePost = MAPPER.readValue(response.asString(),
                                                                                    new TypeReference<ExtendedCourthousePost>(){});
        return extendedCourthousePost.getId();
    }
}
