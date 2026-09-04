package uk.gov.hmcts.darts.authorisation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AnnotationStub;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIAL_CONDUCT;

@AutoConfigureMockMvc
class JudicialConductAccessIntTest extends IntegrationBase {

    private static final OffsetDateTime HEARING_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnnotationStub annotationStub;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "JCO-CASE-1",
            "JCO-COURTHOUSE",
            "jco-courtroom",
            DateConverterUtil.toLocalDateTime(HEARING_DATE_TIME)
        );
        dartsDatabase.createEvent(hearing);
        addMediaToHearing();

        UserAccountEntity requestor = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearing.getCourtroom().getCourthouse());
        dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            requestor,
            hearing.getCourtCase(),
            hearing,
            HEARING_DATE_TIME,
            false
        );
        annotationStub.createAndSaveAnnotationEntityWith(requestor, "JCO should not see this annotation", hearing);

        givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIAL_CONDUCT);
    }

    @Test
    void judicialConductUserCanAccessCaseFileTabsAcrossAllCourts() throws Exception {
        Integer caseId = hearing.getCourtCase().getId();

        mockMvc.perform(get("/cases/{case_id}", caseId))
            .andExpect(status().isOk());

        mockMvc.perform(get("/cases/{case_id}/hearings", caseId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)))
            .andExpect(jsonPath("$[0].id", is(hearing.getId())));

        mockMvc.perform(get("/cases/{case_id}/events", caseId)
                            .queryParam("page_number", "1")
                            .queryParam("page_size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)));

        mockMvc.perform(get("/cases/{case_id}/transcripts", caseId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void judicialConductUserCanAccessHearingDetailsEventsAndAudioAcrossAllCourts() throws Exception {
        Integer hearingId = hearing.getId();

        mockMvc.perform(get("/hearings/{hearingId}", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hearing_id", is(hearingId)));

        mockMvc.perform(get("/hearings/{hearingId}/events", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));

        mockMvc.perform(get("/audio/hearings/{hearing_id}/audios", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void judicialConductUserCannotAccessAnnotations() throws Exception {
        mockMvc.perform(get("/cases/{case_id}/annotations", hearing.getCourtCase().getId()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/hearings/{hearingId}/annotations", hearing.getId()))
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
