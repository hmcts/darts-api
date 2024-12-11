package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TranscriptionControllerSearchTest extends IntegrationBase {

    private static final String ENDPOINT = "/admin/transcriptions/search";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminAndSuperUser(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                post(ENDPOINT)
                    .content("{}")
                    .contentType("application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.INCLUDE)
    void allowsSuperAdminAndSuperUser(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                post(ENDPOINT)
                    .content("{}")
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void adminTranscriptionsSearchPost_shouldReturnBadRequest_whenCourthouseDisplayNameIsLowercase() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        TranscriptionSearchRequest request = new TranscriptionSearchRequest();
        request.setCourthouseDisplayName("london crown court");

        mockMvc.perform(
                post(ENDPOINT)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType("application/json"))
            .andExpect(status().isBadRequest())
            .andExpect(result -> {
                String response = result.getResponse().getContentAsString();
                Assertions.assertTrue(response.contains("Courthouse display name must be uppercase"));
            });
    }

}
