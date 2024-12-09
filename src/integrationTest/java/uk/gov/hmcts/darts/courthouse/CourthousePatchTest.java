package uk.gov.hmcts.darts.courthouse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.RegionTestData.minimalRegion;

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
                    .content(courthouseNamePatch("SOME-NEW-COURTHOUSE-NAME"))
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).extractingJsonPathStringValue("courthouse_name")
            .isEqualTo("SOME-NEW-COURTHOUSE-NAME");
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
        var region = dartsDatabase.save(minimalRegion());
        var courthouse = dartsDatabase.save(someMinimalCourthouse());

        var mvcResult = mockMvc.perform(
                patch(ENDPOINT + courthouse.getId().toString())
                    .content(regionPatch(region.getId()))
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).extractingJsonPathNumberValue("region_id").isEqualTo(region.getId());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdmin(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                patch(ENDPOINT + "1")
                    .content(displayNamePatch("some-new-display-name"))
                    .contentType("application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void updateCourthouse_shouldReturnBadRequest_whenNameIsLowercase() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var courthouse = dartsDatabase.save(someMinimalCourthouse());

        mockMvc.perform(
                patch(ENDPOINT + courthouse.getId().toString())
                    .content(courthouseNamePatch("lowercase-name"))
                    .contentType("application/json"))
            .andExpect(status().isBadRequest());
    }

    private String regionPatch(Integer regionId) {
        return String.format("""
                                 {
                                   "region_id": %d
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
