package uk.gov.hmcts.darts.usermanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

class SecurityGroupFunctionalTest extends FunctionalTest {

    public static final ObjectMapper MAPPER = new ObjectMapper();

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
    void shouldCreateSecurityGroup() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups"))
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "name": "ACME",
                        "display_name": "ACME Transcription Services",
                        "description": "A temporary group created by functional test",
                        "security_role_id": 4
                      }
                      """)
            .post()
            .thenReturn();

        assertEquals(201, response.statusCode());

        JSONAssert.assertEquals(
            """
                {
                  "id": "",
                  "name": "ACME",
                  "display_name": "ACME Transcription Services",
                  "description": "A temporary group created by functional test",
                  "display_state": true,
                  "global_access": false,
                  "security_role_id": 4
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
    void shouldGetSecurityGroupWithIdAndRoleAndUsers() throws JsonProcessingException {
        buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups/4"))
            .contentType(ContentType.JSON)
            .get()
            .then()
            .assertThat()
            .statusCode(200)
            .body("user_ids", notNullValue())
            .body("id", equalTo(4))
            .body("security_role_id", equalTo(SUPER_ADMIN.getId()))
            .body("global_access", equalTo(true))
            .body("display_state", equalTo(true))
            .body("courthouse_ids", hasSize(0))
            .body("name", equalTo("SUPER_ADMIN"))
            .body("display_name", equalTo("Super Admin"));
    }

    @Test
    void shouldGetSecurityGroups() throws JsonProcessingException {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups"))
            .contentType(ContentType.JSON)
            .get();
        assertEquals(200, response.getStatusCode());
        ObjectMapper objectMapper = new ObjectMapper();
        List<SecurityGroupWithIdAndRoleAndUsers> securityGroupWithIdAndRoles = objectMapper.readValue(response.asString(),
                                                                                                      new TypeReference<>() {
                                                                                                      });
        assertFalse(securityGroupWithIdAndRoles.isEmpty());

        List<SecurityGroupWithIdAndRoleAndUsers> staticGroups =
            securityGroupWithIdAndRoles.stream()
                .filter(group -> group.getId() == 4
                    || group.getId() >= -6 && group.getId() <= -1
                    || group.getId() >= 9 && group.getId() <= 13
                    || group.getSecurityRoleId().equals(SecurityRoleEnum.SUPER_USER.getId())
                    || group.getSecurityRoleId().equals(SecurityRoleEnum.DARTS.getId()))
                .toList();

        checkGroup(staticGroups.get(0), "CPP", true, 12, false, null);
        checkGroup(staticGroups.get(1), "DAR_PC", true, 13, false, null);
        checkGroup(staticGroups.get(2), "DARTS", true, 10, false, null);
        checkGroup(staticGroups.get(3), "hmcts_staff_1", false, 3, true, 127);
        checkGroup(staticGroups.get(4), "hmcts_staff_2", false, 2, true, 127);
        checkGroup(staticGroups.get(5), "hmcts_staff_3", false, 1, true, 127);
        checkGroup(staticGroups.get(6), "hmcts_staff_4", false, 4, true, 127);
        checkGroup(staticGroups.get(7), "hmcts_staff_5", true, 5, true, 127);
        checkGroup(staticGroups.get(8), "hmcts_staff_6", true, 6, true, 127);
        checkGroup(staticGroups.get(9), "MEDIA_IN_PERPETUITY", false, 10, true, null);
        checkGroup(staticGroups.get(10), "MID_TIER", true, 14, false, null);
        checkGroup(staticGroups.get(11), "SUPER_ADMIN", true, 8, true, null);
        checkGroup(staticGroups.get(12), "SUPER_USER", true, 7, true, null);
        checkGroup(staticGroups.get(13), "XHIBIT", true, 11, false, null);

    }

    private void checkGroup(SecurityGroupWithIdAndRoleAndUsers group, String name, boolean globalAccess, Integer roleId, boolean displayState,
                            Integer courtroomId) {
        assertEquals(name, group.getName());
        assertEquals(globalAccess, group.getGlobalAccess());
        assertEquals(roleId, group.getSecurityRoleId());
        assertEquals(displayState, group.getDisplayState());
        if (courtroomId != null) {
            assertTrue(group.getCourthouseIds().contains(courtroomId));
        }
    }

    @Test
    void shouldPatchSecurityGroups() throws JsonProcessingException {

        String postContent = """
            {
              "name": "<func-a-security-group>",
              "display_name": "<A security group>",
              "description": "func-test group",
              "security_role_id": 4
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
              "user_ids": [<userId1>,<userId2>]
            }
            """;
        String newName = "func-a-security-group-new-name " + UUID.randomUUID();
        patchContent = patchContent.replace("<func-a-security-group-new-name>", newName);
        String newDisplayName = "A security group new name " + UUID.randomUUID();
        patchContent = patchContent.replace("<A security group new name>", newDisplayName);
        Integer id1 = createCourthouse(("FUNC-A-COURTHOUSE " + UUID.randomUUID()).toUpperCase(Locale.ENGLISH), "func-a-courthouse" + UUID.randomUUID());
        Integer id2 = createCourthouse(("FUNC-A-COURTHOUSE " + UUID.randomUUID()).toUpperCase(Locale.ENGLISH), "func-a-courthouse" + UUID.randomUUID());
        patchContent = patchContent.replace("<id1>", id1.toString());
        patchContent = patchContent.replace("<id2>", id2.toString());
        Response createUserResponse = createUser("user1@email.com");
        int userId1 = new JSONObject(createUserResponse.asString())
            .getInt("id");
        createUserResponse = createUser("user2@email.com");
        int userId2 = new JSONObject(createUserResponse.asString())
            .getInt("id");
        patchContent = patchContent.replace("<userId1>", Integer.toString(userId1));
        patchContent = patchContent.replace("<userId2>", Integer.toString(userId2));

        SecurityGroupWithIdAndRole securityGroupWithIdAndRoles = MAPPER.readValue(response.asString(),
                                                                                  new TypeReference<>() {
                                                                                  });

        response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups/" + securityGroupWithIdAndRoles.getId()))
            .contentType(ContentType.JSON)
            .body(patchContent)
            .patch()
            .thenReturn();

        SecurityGroupWithIdAndRoleAndUsers securityGroupWithIdAndRoleAndUsers = MAPPER.readValue(response.asString(),
                                                                                                 new TypeReference<>() {
                                                                                                 });

        assertEquals(newName, securityGroupWithIdAndRoleAndUsers.getName());
        assertEquals(newDisplayName, securityGroupWithIdAndRoleAndUsers.getDisplayName());
        assertEquals("func-test group new description", securityGroupWithIdAndRoleAndUsers.getDescription());
        assertTrue(securityGroupWithIdAndRoleAndUsers.getCourthouseIds().contains(id1));
        assertTrue(securityGroupWithIdAndRoleAndUsers.getCourthouseIds().contains(id2));
        assertTrue(securityGroupWithIdAndRoleAndUsers.getUserIds().contains(userId1));
        assertTrue(securityGroupWithIdAndRoleAndUsers.getUserIds().contains(userId2));

        response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/security-groups/" + securityGroupWithIdAndRoles.getId()))
            .contentType(ContentType.JSON)
            .get()
            .thenReturn();

        SecurityGroupWithIdAndRole retrievedSecurityGroupWithIdAndRoles = MAPPER.readValue(response.asString(),
                                                                                           new TypeReference<>() {
                                                                                           });

        assertEquals(newName, retrievedSecurityGroupWithIdAndRoles.getName());
        assertEquals(newDisplayName, retrievedSecurityGroupWithIdAndRoles.getDisplayName());
        assertEquals("func-test group new description", retrievedSecurityGroupWithIdAndRoles.getDescription());
        assertTrue(retrievedSecurityGroupWithIdAndRoles.getCourthouseIds().contains(id1));
        assertTrue(retrievedSecurityGroupWithIdAndRoles.getCourthouseIds().contains(id2));
        assertTrue(retrievedSecurityGroupWithIdAndRoles.getUserIds().contains(userId1));
        assertTrue(retrievedSecurityGroupWithIdAndRoles.getUserIds().contains(userId2));


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
                                                                         new TypeReference<>() {
                                                                         });
        return extendedCourthousePost.getId();
    }

    private Response createUser(String email) {
        String request = """
            {
                 "full_name": "James Smith",
                 "email_address": "<email>",
                 "description": "A temporary user created by functional test"
            }
            """;
        request = request.replace("<email>", email);

        Response response = buildRequestWithExternalGlobalAccessAuth()
            .baseUri(getUri("/admin/users"))
            .contentType(ContentType.JSON)
            .body(request)
            .post()
            .thenReturn();

        assertEquals(201, response.getStatusCode());

        return response;
    }
}
