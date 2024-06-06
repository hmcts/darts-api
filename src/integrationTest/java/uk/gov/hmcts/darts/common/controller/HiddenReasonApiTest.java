package uk.gov.hmcts.darts.common.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class HiddenReasonApiTest extends IntegrationBase {

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    private static final URI ENDPOINT = URI.create("/admin/hidden-reasons");

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = INCLUDE)
    void shouldObtainAllHiddenReasons(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        var jsonString = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("""
                                    [
                                      {
                                        "id": 1,
                                        "reason": "PUBLIC_INTEREST_IMMUNITY",
                                        "display_name": "Public interest immunity",
                                        "display_state": false,
                                        "display_order": 1,
                                        "marked_for_deletion": true
                                      },
                                      {
                                        "id": 2,
                                        "reason": "CLASSIFIED",
                                        "display_name": "Classified above official",
                                        "display_state": false,
                                        "display_order": 2,
                                        "marked_for_deletion": true
                                      },
                                      {
                                        "id": 3,
                                        "reason": "OTHER_DELETE",
                                        "display_name": "Other reason to delete",
                                        "display_state": false,
                                        "display_order": 3,
                                        "marked_for_deletion": true
                                      },
                                      {
                                        "id": 4,
                                        "reason": "OTHER_HIDE",
                                        "display_name": "Other reason to hide only",
                                        "display_state": true,
                                        "display_order": 4,
                                        "marked_for_deletion": false
                                      }
                                    ]
                                    """, jsonString, JSONCompareMode.STRICT);
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EXCLUDE)
    void shouldDenyAccess(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isForbidden())
            .andReturn();

        // Then
        var jsonString = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("""
                                    {
                                      "type": "AUTHORISATION_109",
                                      "title": "User is not authorised for this endpoint",
                                      "status": 403
                                    }
                                    """, jsonString, JSONCompareMode.STRICT);
    }

}
