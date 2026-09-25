package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.OffsetDateTime;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TranscriptionControllerAuthorisationIntTest extends IntegrationBase {

    private static final OffsetDateTime HEARING_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GivenBuilder given;
    @MockitoBean
    private TranscriptionService transcriptionService;

    private TranscriptionEntity transcription;

    @BeforeEach
    void setUp() {
        HearingEntity hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "TRANSCRIPTION-AUTH-CASE", "TRANSCRIPTION-AUTH-COURTHOUSE", "transcription-auth-courtroom",
            HEARING_DATE_TIME.toLocalDateTime()
        );
        transcription = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
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
    void allowsRolesWithGlobalTranscriptionAccess(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);
        when(transcriptionService.downloadTranscript(anyLong())).thenReturn(downloadResponse());

        mockMvc.perform(get("/transcriptions/{transcription_id}", transcription.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/transcriptions/{transcription_id}/document", transcription.getId()))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {
        "JUDICIARY", "SUPER_ADMIN", "SUPER_USER", "RCJ_APPEALS", "TRANSLATION_QA", "JUDICIAL_CONDUCT"
    }, mode = EXCLUDE)
    void forbidsRolesWithoutGlobalTranscriptionAccess(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(get("/transcriptions/{transcription_id}", transcription.getId())).andExpect(status().isForbidden());
        mockMvc.perform(get("/transcriptions/{transcription_id}/document", transcription.getId()))
            .andExpect(status().isForbidden());
    }

    private DownloadTranscriptResponse downloadResponse() {
        return DownloadTranscriptResponse.builder()
            .resource(new ByteArrayResource(new byte[0]))
            .fileName("transcript.docx")
            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .transcriptionDocumentId(1L)
            .build();
    }
}
