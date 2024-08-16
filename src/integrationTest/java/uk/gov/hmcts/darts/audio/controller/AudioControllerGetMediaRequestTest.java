package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.MediaRequestTestData.minimalRequestData;

@AutoConfigureMockMvc
class AudioControllerGetMediaRequestTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/admin/media-requests/");
    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EXCLUDE)
    void disallowsAllUsersExceptSuperAdmin(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                get(ENDPOINT + "1")
                    .contentType("application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Disabled("Impacted by V1_363__adding_not_null_constraints_part_4.sql")
    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void allowsSuperAdmin() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var persistedMediaRequest = dartsDatabase.saveWithMediaRequestWithTransientEntities(minimalRequestData());

        mockMvc.perform(
                get(ENDPOINT + String.valueOf(persistedMediaRequest.getId()))
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void throwsNotFoundWhenMediaRequestDoesntExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(
                get(ENDPOINT + "-1")
                    .contentType("application/json"))
            .andExpect(status().isNotFound())
            .andReturn();
    }
}
