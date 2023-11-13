package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.net.URI;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@Transactional
class TranscriptionControllerGetYourTranscriptsIntTest {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions");

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    private TranscriptionEntity transcriptionEntity;
    private UserAccountEntity testUser;
    private UserAccountEntity systemUser;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        transcriptionEntity = authorisationStub.getTranscriptionEntity();

        systemUser = authorisationStub.getSystemUser();
        testUser = authorisationStub.getTestUser();

        when(mockUserIdentity.getEmailAddress()).thenReturn(testUser.getEmailAddress());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void getYourTranscriptsShouldReturnRequesterOnlyOk() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath("$.requester_transcriptions[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath(
                "$.requester_transcriptions[0].case_number",
                is(courtCase.getCaseNumber())
            ))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is("Bristol")))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].urgency", is("Standard")))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts").isString())

            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscriptsShouldReturnOk() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                systemUser.getId()
            );

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions").isEmpty())
            .andExpect(jsonPath("$.approver_transcriptions").isEmpty());
    }

    @Test
    void getYourTranscriptsShouldReturnRequesterAndApproverCombinedOk() throws Exception {
        var courtCase = authorisationStub.getCourtCaseEntity();
        var systemUserTranscription = dartsDatabaseStub.getTranscriptionStub()
            .createAndSaveAwaitingAuthorisationTranscription(
                systemUser,
                courtCase,
                authorisationStub.getHearingEntity(), now(UTC)
            );

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                "user_id",
                testUser.getId()
            );

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_id", is(transcriptionEntity.getId())))
            .andExpect(jsonPath(
                "$.requester_transcriptions[0].case_id",
                is(courtCase.getId())
            ))
            .andExpect(jsonPath(
                "$.requester_transcriptions[0].case_number",
                is(courtCase.getCaseNumber())
            ))
            .andExpect(jsonPath("$.requester_transcriptions[0].courthouse_name", is("Bristol")))
            .andExpect(jsonPath("$.requester_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.requester_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.requester_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.requester_transcriptions[0].urgency", is("Standard")))
            .andExpect(jsonPath("$.requester_transcriptions[0].requested_ts").isString())

            .andExpect(jsonPath("$.approver_transcriptions[0].transcription_id", is(systemUserTranscription.getId())))
            .andExpect(jsonPath(
                "$.approver_transcriptions[0].case_id",
                is(courtCase.getId())
            ))
            .andExpect(jsonPath(
                "$.approver_transcriptions[0].case_number",
                is(courtCase.getCaseNumber())
            ))
            .andExpect(jsonPath("$.approver_transcriptions[0].courthouse_name", is("Bristol")))
            .andExpect(jsonPath("$.approver_transcriptions[0].hearing_date").isString())
            .andExpect(jsonPath("$.approver_transcriptions[0].transcription_type", is("Specified Times")))
            .andExpect(jsonPath("$.approver_transcriptions[0].status", is("Awaiting Authorisation")))
            .andExpect(jsonPath("$.approver_transcriptions[0].urgency", is("Standard")))
            .andExpect(jsonPath("$.approver_transcriptions[0].requested_ts").isString());
    }

}
