package uk.gov.hmcts.darts.authorisation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class HmctsTranscriptionHubAccessIntTest extends IntegrationBase {

    private static final OffsetDateTime HEARING_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "HUB-ACCESS-CASE",
            "HUB-ACCESS-COURTHOUSE",
            "hub-access-courtroom",
            DateConverterUtil.toLocalDateTime(HEARING_DATE_TIME)
        );
        dartsDatabase.createEvent(hearing);
        addMediaToHearing();
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.HMCTS_TRANSCRIPTION_HUB);
    }

    @Test
    void caseEndpoints_shouldReturnOkAndTranscriptListShouldReturnForbidden_whenUserHasHubRole() throws Exception {
        Integer caseId = hearing.getCourtCase().getId();

        mockMvc.perform(get("/cases/{case_id}", caseId))
            .andExpect(status().isOk());
        mockMvc.perform(get("/cases/{case_id}/hearings", caseId))
            .andExpect(status().isOk());
        mockMvc.perform(get("/cases/{case_id}/events", caseId)
                            .queryParam("page_number", "1")
                            .queryParam("page_size", "25"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/cases/{case_id}/transcripts", caseId))
            .andExpect(status().isForbidden());
    }

    @Test
    void hearingAndAudioEndpoints_shouldReturnOkAndTranscriptListShouldReturnForbidden_whenUserHasHubRole() throws Exception {
        Integer hearingId = hearing.getId();

        mockMvc.perform(get("/hearings/{hearing_id}", hearingId))
            .andExpect(status().isOk());
        mockMvc.perform(get("/hearings/{hearing_id}/events", hearingId))
            .andExpect(status().isOk());
        mockMvc.perform(get("/audio/hearings/{hearing_id}/audios", hearingId))
            .andExpect(status().isOk());
        mockMvc.perform(get("/hearings/{hearing_id}/transcripts", hearingId))
            .andExpect(status().isForbidden());
    }

    private void addMediaToHearing() {
        var media = PersistableFactory.getMediaTestData()
            .createMediaWith(hearing.getCourtroom(), MEDIA_START_TIME, MEDIA_END_TIME, 1);
        hearing.addMedia(media);
        dartsPersistence.save(hearing);
        dartsPersistence.save(PersistableFactory.getExternalObjectDirectoryTestData()
                                  .eodStoredInUnstructuredLocationForMedia(media));
    }
}
