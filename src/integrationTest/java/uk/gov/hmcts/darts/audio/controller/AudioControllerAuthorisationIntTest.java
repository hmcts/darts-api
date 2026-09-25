package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AudioControllerAuthorisationIntTest extends IntegrationBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GivenBuilder given;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "AUDIO-AUTH-CASE", "AUDIO-AUTH-COURTHOUSE", "audio-auth-courtroom", LocalDateTime.now()
        );
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "DARTS", "JUDICIAL_CONDUCT"
    }, mode = INCLUDE)
    void allowsRolesWithGlobalAudioAccess(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(get("/audio/hearings/{hearing_id}/audios", hearing.getId()))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "DARTS", "JUDICIAL_CONDUCT"
    }, mode = EXCLUDE)
    void forbidsRolesWithoutGlobalAudioAccess(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(get("/audio/hearings/{hearing_id}/audios", hearing.getId()))
            .andExpect(status().isForbidden());
    }
}
