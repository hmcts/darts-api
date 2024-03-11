package uk.gov.hmcts.darts.courthouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.GivenBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;

@AutoConfigureMockMvc
class CourthousePatchTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/admin/courthouses/");

    private final BasicJsonTester json = new BasicJsonTester(getClass());

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void updatesCourthouseName() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var courthouse = dartsDatabase.save(someMinimalCourthouse());

        var mvcResult = mockMvc.perform(
                patch(ENDPOINT + courthouse.getId().toString())
                    .content(courthouseNamePatch("some-new-courthouse-name"))
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).extractingJsonPathStringValue("courthouse_name")
            .isEqualTo("some-new-courthouse-name");
    }

    @Test
    void updatesDisplayName() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var courthouse = dartsDatabase.save(someMinimalCourthouse());

        var mvcResult = mockMvc.perform(
                patch(ENDPOINT + courthouse.getId().toString())
                    .content(displayNamePatch("some-new-display-name"))
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).extractingJsonPathStringValue("display_name")
            .isEqualTo("some-new-display-name");
    }

    @Test
    void updatesSecurityGroups() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var courthouse = dartsDatabase.save(someMinimalCourthouse());

        var mvcResult = mockMvc.perform(
                patch(ENDPOINT + courthouse.getId().toString())
                    .content(securityGroupsPatch("1"))
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).extractingJsonPathArrayValue("security_group_ids").containsExactly(1);
    }

    @Test
    void updatesRegions() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var courthouse = dartsDatabase.save(someMinimalCourthouse());

        var mvcResult = mockMvc.perform(
                patch(ENDPOINT + courthouse.getId().toString())
                    .content(regionPatch(1))
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).extractingJsonPathNumberValue("region_id").isEqualTo(1);
    }

    private String regionPatch(Integer regionId) {
         return String.format("""
            {
              "region_id": %n
            }""", regionId);
    }

    private String courthouseNamePatch(String name) {
        return String.format("""
            {
              "courthouse_name": "%s"
            }""", name);
    }

    private String displayNamePatch(String displayName) {
        return String.format("""
            {
              "display_name": "%s"
            }""", displayName);
    }

    private String securityGroupsPatch(String grpIds) {
        return String.format("""
            {
              "security_group_ids": [ %s ]
            }""", grpIds);
    }
}
