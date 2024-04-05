package uk.gov.hmcts.darts.usermanagement.controller;


import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.testutils.data.SecurityGroupTestData.buildGroupForRole;
import static uk.gov.hmcts.darts.testutils.data.UserAccountTestData.minimalUserAccount;

@AutoConfigureMockMvc
class PatchSecurityGroupIntTest extends IntegrationBase {
    private static final String ORIGINAL_DESCRIPTION = "Security group description original";
    private static final String NEW_DISPLAY_NAME = "Security group display name new";

    private static final String TEST_COURTHOUSE_NAME_1 = "Courthouse name 1";
    private static final String TEST_COURTHOUSE_NAME_2 = "Courthouse name 2";
    private static final String NEW_DESCRIPTION = "Security group description new";


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockBean
    private UserIdentity userIdentity;

    @Test
    void patchSecurityGroupShouldSucceedWhenProvidedWithValidValueForSubsetOfAllowableFields() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        String name = "security group name" + UUID.randomUUID();
        String displayName = "security group display name" + UUID.randomUUID();
        Integer id = createSecurityGroup(name, displayName);

        String patchContent = """
            {
              "name": "<name>",
              "display_name": "<display_name>"
            }
              """;
        String newName = "security group name" + UUID.randomUUID();
        patchContent = patchContent.replace("<name>", newName);
        patchContent = patchContent.replace("<display_name>", NEW_DISPLAY_NAME);

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value(newName))
            .andExpect(jsonPath("$.display_name").value(NEW_DISPLAY_NAME))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.global_access").value(false))
            .andExpect(jsonPath("$.security_role_id").isNumber())
            .andExpect(jsonPath("$.courthouse_ids").isEmpty())
            .andExpect(jsonPath("$.user_ids").isEmpty())
            .andReturn();
    }


    @Test
    void patchSecurityGroupShouldSucceedWhenProvidedWithValidValuesForAllAllowableFields() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);


        String patchContent = """
            {
              "name": "<name>",
              "display_name": "<display_name>",
              "description": "<description>"
            }
              """;
        String newName = "security group name" + UUID.randomUUID();
        patchContent = patchContent.replace("<name>", newName);
        String newDisplayName = "Security group display name new " + UUID.randomUUID();
        patchContent = patchContent.replace("<display_name>", newDisplayName);
        patchContent = patchContent.replace("<description>", NEW_DESCRIPTION);

        String name = "security group name" + UUID.randomUUID();
        String displayName = "security group display name" + UUID.randomUUID();
        Integer id = createSecurityGroup(name, displayName);

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value(newName))
            .andExpect(jsonPath("$.display_name").value(newDisplayName))
            .andExpect(jsonPath("$.description").value(NEW_DESCRIPTION))
            .andExpect(jsonPath("$.global_access").value(false))
            .andExpect(jsonPath("$.security_role_id").isNumber())
            .andExpect(jsonPath("$.courthouse_ids").isEmpty())
            .andExpect(jsonPath("$.user_ids").isEmpty())
            .andReturn();
    }

    @Test
    void patchSecurityGroupShouldSucceedWhenProvidedWithCourthouseIds() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        String name = "security group name" + UUID.randomUUID();
        String displayName = "security group display name" + UUID.randomUUID();
        Integer id = createSecurityGroup(name, displayName);

        var courthouseEntity1 = dartsDatabase.createCourthouseUnlessExists(TEST_COURTHOUSE_NAME_1);
        var courthouseEntity2 = dartsDatabase.createCourthouseUnlessExists(TEST_COURTHOUSE_NAME_2);

        String patchContent = String.format("{\"courthouse_ids\": [%s, %s]}", courthouseEntity1.getId(), courthouseEntity2.getId());

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.display_name").value(displayName))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.global_access").value(false))
            .andExpect(jsonPath("$.security_role_id").isNumber())
            .andExpect(jsonPath("$.courthouse_ids[0]").value(courthouseEntity1.getId()))
            .andExpect(jsonPath("$.courthouse_ids[1]").value(courthouseEntity2.getId()))
            .andExpect(jsonPath("$.user_ids").isEmpty())
            .andReturn();
    }

    @Test
    void patchSecurityGroupShouldSucceedWhenProvidedWithUserIds() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(userIdentity);
        String name = "security group name" + UUID.randomUUID();
        String displayName = "security group display name" + UUID.randomUUID();
        Integer id = createSecurityGroup(name, displayName);

        UserAccountEntity user1 = createEnabledUserAccountEntity(user, "email1");
        UserAccountEntity user2 = createEnabledUserAccountEntity(user, "email2");

        String patchContent = String.format("{\"user_ids\": [%s, %s]}", user1.getId(), user2.getId());

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.display_name").value(displayName))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.global_access").value(false))
            .andExpect(jsonPath("$.security_role_id").isNumber())
            .andExpect(jsonPath("$.user_ids[0]").value(user1.getId()))
            .andExpect(jsonPath("$.user_ids[1]").value(user2.getId()))
            .andExpect(jsonPath("$.courthouse_ids").isEmpty())
            .andReturn();
    }

    @Test
    void patchSecurityGroupsShouldFailWithUnauthorizedUser() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        String patchContent = """
            {
              "name": "<name>",
              "display_name": "<display_name>"
            }
              """;
        String newName = "security group name" + UUID.randomUUID();
        patchContent = patchContent.replace("<name>", newName);
        String newDisplayName = "Security group display name new " + UUID.randomUUID();
        patchContent = patchContent.replace("<display_name>", newDisplayName);

        String name = "security group name" + UUID.randomUUID();
        String displayName = "security group display name" + UUID.randomUUID();
        Integer id = createSecurityGroup(name, displayName);

        superAdminUserStub.givenUserIsNotAuthorised(userIdentity);
        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void patchSecurityShouldFailWithInvalidSecurityGroupId() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        Integer id = -1000;

        String patchContent = """
            {
              "name": "<name>",
              "display_name": "<display_name>"
            }
              """;
        String newName = "security group name" + UUID.randomUUID();
        patchContent = patchContent.replace("<name>", newName);
        String newDisplayName = "Security group display name new " + UUID.randomUUID();
        patchContent = patchContent.replace("<display_name>", newDisplayName);

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    void patchSecurityGroupShouldFailWithInvalidCourthouseId() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);


        Integer id = createSecurityGroup("security group name" + UUID.randomUUID(), "security group display name" + UUID.randomUUID());

        String patchContent = """
            {
              "courthouse_ids": [-100]
            }
              """;

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    void patchSecurityGroupShouldFailWithInvalidUserId() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        Integer id = createSecurityGroup("security group name " + UUID.randomUUID(), "security group name" + UUID.randomUUID());

        String patchContent = """
            {
              "user_ids": [-100]
            }
              """;

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    void patchSecurityGroupShouldFailWhenProvidedWithExistingName() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        String name1 = "security group name " + UUID.randomUUID();
        createSecurityGroup(name1, "security group name" + UUID.randomUUID());
        String name2 = "security group name " + UUID.randomUUID();
        Integer id2 = createSecurityGroup(name2, "security group name" + UUID.randomUUID());

        String patchContent = """
            {
              "name": "<name>",
              "display_name": "<display_name>"
            }
              """;
        patchContent = patchContent.replace("<name>", name1);
        patchContent = patchContent.replace("<display_name>", NEW_DISPLAY_NAME);

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id2)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isConflict())
            .andReturn();
    }

    @Test
    void patchSecurityGroupShouldFailWhenProvidedWithExistingDisplayName() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        String name1 = "security group name " + UUID.randomUUID();
        String displayName1 = "security group display name " + UUID.randomUUID();
        createSecurityGroup(name1, displayName1);
        String name2 = "security group name " + UUID.randomUUID();
        String displayName2 = "security group display name " + UUID.randomUUID();
        Integer id2 = createSecurityGroup(name2, displayName2);

        String patchContent = """
            {
              "display_name": "<display_name>"
            }
              """;
        patchContent = patchContent.replace("<display_name>", displayName1);

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id2)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isConflict())
            .andReturn();
    }

    @Test
    void removeAllUsersGivenEmptyUserPatchList() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var securityGroupEntity = buildGroupForRole(APPROVER);
        dartsDatabase.addUserToGroup(minimalUserAccount(), securityGroupEntity);
        dartsDatabase.addUserToGroup(minimalUserAccount(), securityGroupEntity);

        mockMvc.perform(
                buildPatchRequest(securityGroupEntity.getId())
                    .content("""
                                     {"user_ids": []}
                                 """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user_ids").isEmpty())
            .andReturn();
    }

    @Test
    void removesOnlyAbsentUsersGivenPatchWithSubsetOfCurrentUsers() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var securityGroupEntity = buildGroupForRole(APPROVER);
        var userAccount1 = minimalUserAccount();
        var userAccount2 = minimalUserAccount();
        dartsDatabase.addUserToGroup(userAccount1, securityGroupEntity);
        dartsDatabase.addUserToGroup(userAccount2, securityGroupEntity);

        mockMvc.perform(
                buildPatchRequest(securityGroupEntity.getId())
                    .content("""
                                     {"user_ids": [%s]}
                                 """.formatted(userAccount1.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user_ids[0]").value(userAccount1.getId()))
            .andReturn();
    }


    private MockHttpServletRequestBuilder buildPostRequest() {
        return post("/admin/security-groups")
            .header("Content-Type", "application/json");
    }

    private MockHttpServletRequestBuilder buildPatchRequest(int id) {
        return patch("/admin/security-groups/" + id)
            .header("Content-Type", "application/json");
    }

    private Integer createSecurityGroup(String name, String displayName) throws Exception {
        String content = """
            {
              "name": "<name>",
              "display_name": "<display_name>",
              "description": "<description>",
              "security_role_id": 4
            }
              """;
        content = content.replace("<name>", name);
        content = content.replace("<display_name>", displayName);
        content = content.replace("<description>", ORIGINAL_DESCRIPTION);

        MockHttpServletRequestBuilder request = buildPostRequest()
            .content(content);

        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.display_name").value(displayName))
            .andExpect(jsonPath("$.description").value(ORIGINAL_DESCRIPTION))
            .andExpect(jsonPath("$.display_state").value(true))
            .andExpect(jsonPath("$.global_access").value(false))
            .andExpect(jsonPath("$.security_role_id").isNumber())
            .andReturn();

        return new JSONObject(result.getResponse().getContentAsString())
            .getInt("id");
    }

    private UserAccountEntity createEnabledUserAccountEntity(UserAccountEntity user, String email) {
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUserName("user name");
        userAccountEntity.setUserFullName("user full name");
        userAccountEntity.setEmailAddress(email);
        userAccountEntity.setUserDescription("Description");
        userAccountEntity.setActive(true);

        userAccountEntity.setIsSystemUser(false);
        userAccountEntity.setCreatedBy(user);
        userAccountEntity.setLastModifiedBy(user);

        return dartsDatabase.getUserAccountRepository()
            .save(userAccountEntity);
    }
}
