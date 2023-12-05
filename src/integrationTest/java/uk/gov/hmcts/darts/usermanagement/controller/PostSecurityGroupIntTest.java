package uk.gov.hmcts.darts.usermanagement.controller;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

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
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private MockMvc mockMvc;

    private TransactionTemplate transactionTemplate;


    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void createSecurityGroupShouldSucceedWhenProvidedWithValidValuesForMinRequiredFields() throws Exception {
        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "ACME",
                           "display_name": "ACME Transcription Services"
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
            .andExpect(jsonPath("$.role_id").isNumber())
            .andReturn();

        var id = new JSONObject(result.getResponse().getContentAsString())
            .getInt("id");

        transactionTemplate.execute(status -> {
            var createdSecurityGroupEntity = securityGroupRepository.findById(id)
                .orElseThrow();

            assertEquals("ACME", createdSecurityGroupEntity.getGroupName());
            assertEquals("ACME Transcription Services", createdSecurityGroupEntity.getGroupDisplayName());
            assertNull(createdSecurityGroupEntity.getDescription());
            assertFalse(createdSecurityGroupEntity.getGlobalAccess());
            assertTrue(createdSecurityGroupEntity.getDisplayState());
            assertEquals("TRANSCRIBER", createdSecurityGroupEntity.getSecurityRoleEntity().getRoleName());

            return null;
        });
    }

    @Test
    void createSecurityGroupShouldSucceedWhenProvidedWithValidValuesForAllFields() throws Exception {
        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "Scribe It",
                           "display_name": "Scribe It Transcription Services",
                           "description": "A test group"
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
            .andExpect(jsonPath("$.role_id").isNumber())
            .andReturn();

        var id = new JSONObject(result.getResponse().getContentAsString())
            .getInt("id");

        transactionTemplate.execute(status -> {
            var createdSecurityGroupEntity = securityGroupRepository.findById(id)
                .orElseThrow();

            assertEquals("Scribe It", createdSecurityGroupEntity.getGroupName());
            assertEquals("Scribe It Transcription Services", createdSecurityGroupEntity.getGroupDisplayName());
            assertEquals(DESCRIPTION, createdSecurityGroupEntity.getDescription());
            assertFalse(createdSecurityGroupEntity.getGlobalAccess());
            assertTrue(createdSecurityGroupEntity.getDisplayState());
            assertEquals("TRANSCRIBER", createdSecurityGroupEntity.getSecurityRoleEntity().getRoleName());

            return null;
        });
    }

    @Test
    void createSecurityGroupShouldFailWhenRequiredFieldsAreMissing() throws Exception {
        MockHttpServletRequestBuilder request = buildRequest()
            .content("""
                         {
                           "name": "ACME"
                         }
                           """);

        mockMvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    void createSecurityGroupShouldFailWhenAttemptingToCreateGroupThatAlreadyExists() throws Exception {
        MockHttpServletRequestBuilder requestForInitialGroup = buildRequest()
            .content("""
                         {
                           "name": "Weyland",
                           "display_name": "Weyland Transcription Services"
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
                           "display_name": "Trying to create a group whose name already exists"
                         }
                           """);
        mockMvc.perform(requestForDuplicateGroup)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("USER_MANAGEMENT_110"))
            .andExpect(jsonPath("$.existing_group_id").value(initialSecurityGroup.get("id")));
    }

    private MockHttpServletRequestBuilder buildRequest() {
        return post("/security-groups")
            .header("Content-Type", "application/json");
    }

}
