package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.net.URI;
import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.when;

@AutoConfigureMockMvc
public class TranscriptionControllerGetTranscriptionWorkflowsIntTest {

    private static final URI ENDPOINT_URI = URI.create("/admin/transcription-workflows");
    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private TranscriptionStub transcriptionStub;
    @Autowired
    private MockMvc mockMvc;
    private TranscriptionEntity transcriptionEntity;
    private UserAccountEntity testUser;
    private UserAccountEntity systemUser;
    @MockBean
    private UserIdentity mockUserIdentity;
    private Integer transcriptionId;

    private static final OffsetDateTime YESTERDAY = now(UTC).minusDays(1).withHour(9).withMinute(0)
        .withSecond(0).withNano(0);

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        transcriptionEntity = authorisationStub.getTranscriptionEntity();

        systemUser = authorisationStub.getSystemUser();
        testUser = authorisationStub.getTestUser();

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void getTranscriptionWorkflowsShouldReturnOkSuccess() {
        
    }
}
