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

@AutoConfigureMockMvc
class PatchSecurityGroupIntTest extends IntegrationBase {
    private static final String ORIGINAL_DISPLAY_NAME = "Security group display name original";
    private static final String ORIGINAL_DESCRIPTION = "Security group description original";
    private static final String NEW_NAME = "Security group name new";
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
        Integer id = createSecurityGroup(name);

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
        patchContent = patchContent.replace("<display_name>", NEW_DISPLAY_NAME);
        patchContent = patchContent.replace("<description>", NEW_DESCRIPTION);

        String name = "security group name" + UUID.randomUUID();
        Integer id = createSecurityGroup(name);

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value(newName))
            .andExpect(jsonPath("$.display_name").value(NEW_DISPLAY_NAME))
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
        Integer id = createSecurityGroup(name);

        var courthouseEntity1 = dartsDatabase.createCourthouseUnlessExists(TEST_COURTHOUSE_NAME_1);
        var courthouseEntity2 = dartsDatabase.createCourthouseUnlessExists(TEST_COURTHOUSE_NAME_2);

        String patchContent = String.format("{\"courthouse_ids\": [%s, %s]}", courthouseEntity1.getId(), courthouseEntity2.getId());

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.display_name").value(ORIGINAL_DISPLAY_NAME))
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
        Integer id = createSecurityGroup(name);

        UserAccountEntity user1 = createEnabledUserAccountEntity(user, "email1");
        UserAccountEntity user2 = createEnabledUserAccountEntity(user, "email2");

        String patchContent = String.format("{\"user_ids\": [%s, %s]}", user1.getId(), user2.getId());

            MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.display_name").value(ORIGINAL_DISPLAY_NAME))
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
        patchContent = patchContent.replace("<display_name>", NEW_DISPLAY_NAME);

        String name = "security group name" + UUID.randomUUID();
        Integer id = createSecurityGroup(name);

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
        patchContent = patchContent.replace("<display_name>", NEW_DISPLAY_NAME);

        MockHttpServletRequestBuilder patchRequest = buildPatchRequest(id)
            .content(patchContent);

        mockMvc.perform(patchRequest)
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    void patchSecurityGroupShouldFailWithInvalidCourthouseId() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);


        Integer id = createSecurityGroup("security group name" + UUID.randomUUID());

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

        Integer id = createSecurityGroup("security group name");

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
        createSecurityGroup(name1);
        String name2 = "security group name " + UUID.randomUUID();
        Integer id2 = createSecurityGroup(name2);

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

    private MockHttpServletRequestBuilder buildPostRequest() {
        return post("/admin/security-groups")
            .header("Content-Type", "application/json");
    }

    private MockHttpServletRequestBuilder buildPatchRequest(int id) {
        return patch("/admin/security-groups/" + id)
            .header("Content-Type", "application/json");
    }

    private Integer createSecurityGroup(String name) throws Exception {
        String content = """
                         {
                           "name": "<name>",
                           "display_name": "<display_name>",
                           "description": "<description>"
                           
                         }
                           """;
        content = content.replace("<name>", name);
        content = content.replace("<display_name>", ORIGINAL_DISPLAY_NAME);
        content = content.replace("<description>", ORIGINAL_DESCRIPTION);

        MockHttpServletRequestBuilder request = buildPostRequest()
            .content(content);

        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.display_name").value(ORIGINAL_DISPLAY_NAME))
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
