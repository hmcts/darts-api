package uk.gov.hmcts.darts.cases.controller;

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
import java.time.OffsetDateTime;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CaseControllerAuthorisationIntTest extends IntegrationBase {

    private static final OffsetDateTime HEARING_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GivenBuilder given;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "CASE-AUTH-CASE", "CASE-AUTH-COURTHOUSE", "case-auth-courtroom", LocalDateTime.now()
        );
        dartsDatabase.createEvent(hearing);
        dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            dartsDatabase.getUserAccountStub().createAuthorisedIntegrationTestUser(hearing.getCourtroom().getCourthouse()),
            hearing.getCourtCase(),
            hearing,
            HEARING_DATE_TIME,
            false
        );
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "DARTS", "JUDICIAL_CONDUCT"
    }, mode = INCLUDE)
    void allowsRolesWithGlobalCaseAccess(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(get("/cases/{case_id}", hearing.getCourtCase().getId())).andExpect(status().isOk());
        mockMvc.perform(get("/cases/{case_id}/hearings", hearing.getCourtCase().getId())).andExpect(status().isOk());
        mockMvc.perform(get("/cases/{case_id}/events", hearing.getCourtCase().getId())
                            .queryParam("page_number", "1").queryParam("page_size", "25"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/cases/{case_id}/transcripts", hearing.getCourtCase().getId())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "DARTS", "JUDICIAL_CONDUCT"
    }, mode = EXCLUDE)
    void forbidsRolesWithoutGlobalCaseAccess(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(get("/cases/{case_id}", hearing.getCourtCase().getId())).andExpect(status().isForbidden());
        mockMvc.perform(get("/cases/{case_id}/hearings", hearing.getCourtCase().getId())).andExpect(status().isForbidden());
        mockMvc.perform(get("/cases/{case_id}/events", hearing.getCourtCase().getId())
                            .queryParam("page_number", "1").queryParam("page_size", "25"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/cases/{case_id}/transcripts", hearing.getCourtCase().getId())).andExpect(status().isForbidden());
    }
}
