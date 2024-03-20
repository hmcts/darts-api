package uk.gov.hmcts.darts.usermanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

class SecurityGroupFunctionalTest extends FunctionalTest {

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
                        "description": "A temporary group created by functional test"
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
            .baseUri(getUri("/admin/security-groups/1"))
            .contentType(ContentType.JSON)
            .get()
            .then()
            .assertThat()
            .statusCode(200)
            .body("user_ids", notNullValue())
            .body("id", equalTo(1))
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
            .get()
            .thenReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<SecurityGroupWithIdAndRole> securityGroupWithIdAndRoles = objectMapper.readValue(response.asString(),
                                                                                              new TypeReference<List<SecurityGroupWithIdAndRole>>() {
                                                                                              });
        assertFalse(securityGroupWithIdAndRoles.isEmpty());

        List<SecurityGroupWithIdAndRole> staticGroups =
            securityGroupWithIdAndRoles.stream()
                .filter(group -> group.getId() == 1
                    || group.getId() >= -6 && group.getId() <= -1
                    || group.getId() >= -17 && group.getId() <= -14
                    || group.getSecurityRoleId() == SecurityRoleEnum.SUPER_USER.getId())
                .sorted(Comparator.comparingInt(SecurityGroupWithIdAndRole::getId).reversed())
                .toList();

        checkGroup(staticGroups.get(0), "SUPER_USER", true, 12, true, null);
        checkGroup(staticGroups.get(1), "SUPER_ADMIN", true, 11, true, null);
        checkGroup(staticGroups.get(2), "hmcts_staff_1", false, 1, true, 1);
        checkGroup(staticGroups.get(3), "hmcts_staff_2", false, 2, true, 1);
        checkGroup(staticGroups.get(4), "hmcts_staff_3", false, 3, true, 1);
        checkGroup(staticGroups.get(5), "hmcts_staff_4", false, 4, true, 1);
        checkGroup(staticGroups.get(6), "hmcts_staff_5", true, 5, true, 1);
        checkGroup(staticGroups.get(7), "hmcts_staff_6", true, 6, true, 1);
        checkGroup(staticGroups.get(8), "Xhibit Group", true, 7, true, 1);
        checkGroup(staticGroups.get(9), "Cpp Group", true, 8, true, 1);
        checkGroup(staticGroups.get(10), "Dar Pc Group", true, 9, true, 1);
        checkGroup(staticGroups.get(11), "Mid Tier Group", true, 10, true, 1);

    }

    private void checkGroup(SecurityGroupWithIdAndRole group, String name, boolean globalAccess, Integer roleId, boolean displayState,
                            Integer courtroomId) {
        assertEquals(name, group.getName());
        assertEquals(globalAccess, group.getGlobalAccess());
        assertEquals(roleId, group.getSecurityRoleId());
        assertEquals(displayState, group.getDisplayState());
        if (courtroomId != null) {
            assertTrue(group.getCourthouseIds().contains(courtroomId));
        }
    }
}
