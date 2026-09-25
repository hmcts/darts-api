package uk.gov.hmcts.darts.hearings.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class HearingsControllerAuthorisationIntTest extends IntegrationBase {

    private static final OffsetDateTime MEDIA_START = OffsetDateTime.parse("2023-01-01T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GivenBuilder given;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "HEARING-AUTH-CASE", "HEARING-AUTH-COURTHOUSE", "hearing-auth-courtroom", LocalDateTime.now()
        );
        dartsDatabase.createEvent(hearing);
        MediaEntity media = PersistableFactory.getMediaTestData()
            .createMediaWith(hearing.getCourtroom(), MEDIA_START, MEDIA_START.plusHours(1), 1);
        hearing.addMedia(media);
        dartsPersistence.save(hearing);
        dartsPersistence.save(PersistableFactory.getExternalObjectDirectoryTestData()
                                  .eodStoredInUnstructuredLocationForMedia(media));
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "DARTS", "JUDICIAL_CONDUCT"
    }, mode = INCLUDE)
    void allowsRolesWithGlobalHearingAccess(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(get("/hearings/{hearing_id}", hearing.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/hearings/{hearing_id}/events", hearing.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/hearings/{hearing_id}/transcripts", hearing.getId())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "DARTS", "JUDICIAL_CONDUCT"
    }, mode = EXCLUDE)
    void forbidsRolesWithoutGlobalHearingAccess(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(get("/hearings/{hearing_id}", hearing.getId())).andExpect(status().isForbidden());
        mockMvc.perform(get("/hearings/{hearing_id}/events", hearing.getId())).andExpect(status().isForbidden());
        mockMvc.perform(get("/hearings/{hearing_id}/transcripts", hearing.getId())).andExpect(status().isForbidden());
    }
}
