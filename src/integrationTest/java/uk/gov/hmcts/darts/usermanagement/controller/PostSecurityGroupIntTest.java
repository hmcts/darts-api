package uk.gov.hmcts.darts.usermanagement.controller;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity_;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PostSecurityGroupIntTest extends IntegrationBase {

    private static final String DESCRIPTION = "A test group";

    @Autowired
    private TransactionalUtil transactionalUtil;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity userIdentity;

    private int maxSecurityGroupId;

    @BeforeEach
    void setUp() {
        openInViewUtil.openEntityManager();
        List<SecurityGroupEntity> securityGroupEntities = securityGroupRepository.findAll(Sort.by(SecurityGroupEntity_.ID).descending());
        maxSecurityGroupId = securityGroupEntities.getFirst().getId();
    }

    @AfterEach
    void tearDown() {
        List<SecurityGroupEntity> securityGroupEntities = securityGroupRepository.findAll(Sort.by(SecurityGroupEntity_.ID).descending());
        for (SecurityGroupEntity securityGroup : securityGroupEntities) {
            if (securityGroup.getId() > maxSecurityGroupId) {
                securityGroupRepository.delete(securityGroup);
            }
        }
        openInViewUtil.closeEntityManager();
    }

    @Test
    void createSecurityGroupShouldSucceedAndSetInterpreterFlagFalseForNonTranslationRole() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "ACME",
                           "display_name": "ACME Transcription Services",
                           "security_role_id": 4
                         }
                           """);

        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("ACME"))
            .andExpect(jsonPath("$.display_name").value("ACME Transcription Services"))
            .andExpect(jsonPath("$.description").doesNotExist())
            .andExpect(jsonPath("$.display_state").value(true))
            .andExpect(jsonPath("$.global_access").value(false))
            .andExpect(jsonPath("$.security_role_id").isNumber())
            .andReturn();

        var id = new JSONObject(result.getResponse().getContentAsString())
            .getInt("id");

        transactionalUtil.inTransaction(() -> {
            var createdSecurityGroupEntity = securityGroupRepository.findById(id)
                .orElseThrow();

            assertEquals("ACME", createdSecurityGroupEntity.getGroupName());
            assertEquals("ACME Transcription Services", createdSecurityGroupEntity.getDisplayName());
            assertNull(createdSecurityGroupEntity.getDescription());
            assertFalse(createdSecurityGroupEntity.getGlobalAccess());
            assertTrue(createdSecurityGroupEntity.getDisplayState());
            assertEquals("TRANSCRIBER", createdSecurityGroupEntity.getSecurityRoleEntity().getRoleName());
            assertFalse(createdSecurityGroupEntity.getUseInterpreter());
        });
    }

    @Test
    void createSecurityGroupShouldSucceedAndSetInterpreterFlagTrueForTranslationRole() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "ACME",
                           "display_name": "ACME Transcription Services",
                           "security_role_id": 5
                         }
                           """);

        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("ACME"))
            .andExpect(jsonPath("$.display_name").value("ACME Transcription Services"))
            .andExpect(jsonPath("$.description").doesNotExist())
            .andExpect(jsonPath("$.display_state").value(true))
            .andExpect(jsonPath("$.global_access").value(false))
            .andExpect(jsonPath("$.security_role_id").isNumber())
            .andReturn();

        var id = new JSONObject(result.getResponse().getContentAsString())
            .getInt("id");

        transactionalUtil.inTransaction(() -> {
            var createdSecurityGroupEntity = securityGroupRepository.findById(id)
                .orElseThrow();

            assertEquals("ACME", createdSecurityGroupEntity.getGroupName());
            assertEquals("ACME Transcription Services", createdSecurityGroupEntity.getDisplayName());
            assertNull(createdSecurityGroupEntity.getDescription());
            assertFalse(createdSecurityGroupEntity.getGlobalAccess());
            assertTrue(createdSecurityGroupEntity.getDisplayState());
            assertEquals("TRANSLATION_QA", createdSecurityGroupEntity.getSecurityRoleEntity().getRoleName());
            assertTrue(createdSecurityGroupEntity.getUseInterpreter());
        });
    }

    @Test
    void createSecurityGroupShouldSucceedWhenProvidedWithValidValuesForAllFields() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "Scribe It",
                           "display_name": "Scribe It Transcription Services",
                           "description": "A test group",
                           "security_role_id": 4
                         }
                           """);

        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Scribe It"))
            .andExpect(jsonPath("$.display_name").value("Scribe It Transcription Services"))
            .andExpect(jsonPath("$.description").value(DESCRIPTION))
            .andExpect(jsonPath("$.display_state").value(true))
            .andExpect(jsonPath("$.global_access").value(false))
            .andExpect(jsonPath("$.security_role_id").isNumber())
            .andReturn();

        var id = new JSONObject(result.getResponse().getContentAsString())
            .getInt("id");

        transactionalUtil.inTransaction(() -> {
            var createdSecurityGroupEntity = securityGroupRepository.findById(id)
                .orElseThrow();

            assertEquals("Scribe It", createdSecurityGroupEntity.getGroupName());
            assertEquals("Scribe It Transcription Services", createdSecurityGroupEntity.getDisplayName());
            assertEquals(DESCRIPTION, createdSecurityGroupEntity.getDescription());
            assertFalse(createdSecurityGroupEntity.getGlobalAccess());
            assertTrue(createdSecurityGroupEntity.getDisplayState());
            assertEquals("TRANSCRIBER", createdSecurityGroupEntity.getSecurityRoleEntity().getRoleName());
        });
    }

    @Test
    void createSecurityGroupShouldFailWhenRequiredFieldsAreMissing() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "ACME",
                           "security_role_id": 4
                         }
                           """);

        mockMvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    void createSecurityGroupShouldFailWhenAttemptingToCreateGroupThatAlreadyExists() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestForInitialGroup = buildRequest()
            .content("""
                         {
                           "name": "Weyland",
                           "display_name": "Weyland Transcription Services",
                           "security_role_id": 4
                         }
                           """);
        MvcResult initialResponse = mockMvc.perform(requestForInitialGroup)
            .andExpect(status().isCreated())
            .andReturn();
        JSONObject initialSecurityGroup = new JSONObject(initialResponse.getResponse()
                                                             .getContentAsString());

        MockHttpServletRequestBuilder requestForDuplicateGroup = buildRequest()
            .content("""
                         {
                           "name": "Weyland",
                           "display_name": "Trying to create a group whose name already exists",
                           "security_role_id": 4
                         }
                           """);
        mockMvc.perform(requestForDuplicateGroup)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("USER_MANAGEMENT_105"))
            .andExpect(jsonPath("$.existing_group_id").value(initialSecurityGroup.get("id")));
    }

    @Test
    void createSecurityGroupShouldFailWhenAttemptingToCreateGroupThatDisplayNameAlreadyExists() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder requestForInitialGroup = buildRequest()
            .content("""
                         {
                           "name": "Weyland",
                           "display_name": "Weyland Transcription Services",
                           "security_role_id": 4
                         }
                           """);
        MvcResult initialResponse = mockMvc.perform(requestForInitialGroup)
            .andExpect(status().isCreated())
            .andReturn();
        JSONObject initialSecurityGroup = new JSONObject(initialResponse.getResponse()
                                                             .getContentAsString());

        MockHttpServletRequestBuilder requestForDuplicateGroup = buildRequest()
            .content("""
                         {
                           "name": "Weyland-new",
                           "display_name": "Weyland Transcription Services",
                           "security_role_id": 4
                         }
                           """);
        mockMvc.perform(requestForDuplicateGroup)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("USER_MANAGEMENT_107"))
            .andExpect(jsonPath("$.existing_group_id").value(initialSecurityGroup.get("id")))
            .andExpect(jsonPath("$.detail").value("Attempt to create group with a display name that already exists"));
    }

    @Test
    void createSecurityGroupShouldFailIfUserIsNotAuthorised() throws Exception {
        superAdminUserStub.givenUserIsNotAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "ACME",
                           "display_name": "ACME Transcription Services",
                           "security_role_id": 4
                         }
                           """);
        mockMvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    void createSecurityGroupShouldFailIfRoleNotProvided() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "ACME",
                           "display_name": "ACME Transcription Services"
                         }
                           """);
        mockMvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    void createSecurityGroupShouldFailIfRoleNotAllowed() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "ACME",
                           "display_name": "ACME Transcription Services",
                           "security_role_id": 3
                         }
                           """);
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("USER_MANAGEMENT_106"))
            .andExpect(jsonPath("$.detail").value(
                "A group with a role of type APPROVER has been requested, but only roles of type [TRANSCRIBER, TRANSLATION_QA] are allowed."));
    }

    private MockHttpServletRequestBuilder buildRequest() {
        return post("/admin/security-groups")
            .header("Content-Type", "application/json");
    }

}
