package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.*;
import com.jayway.jsonpath.*;
import org.junit.jupiter.api.*;
import org.skyscreamer.jsonassert.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import org.springframework.test.web.servlet.result.*;
import org.springframework.transaction.annotation.*;
import uk.gov.hmcts.darts.authorisation.component.*;
import uk.gov.hmcts.darts.common.entity.*;
import uk.gov.hmcts.darts.testutils.*;
import uk.gov.hmcts.darts.testutils.data.*;
import uk.gov.hmcts.darts.testutils.stubs.*;
import uk.gov.hmcts.darts.transcriptions.model.*;

import java.net.*;
import java.time.*;
import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.hmcts.darts.audiorequests.model.MediaRequestStatus.EXPIRED;
import static uk.gov.hmcts.darts.common.entity.TranscriptionEntity_.transcriptionDocument;
import static uk.gov.hmcts.darts.testutils.TestUtils.*;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@AutoConfigureMockMvc
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TranscriptionControllerGetTranscriptionTest extends IntegrationBase {



    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    private Integer transcriptionId;
    private Integer testUserId;


    @BeforeEach
    @Transactional
    void beforeEach() {
        authorisationStub.givenTestSchema();

        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        TranscriptionStub transcriptionStub = dartsDatabaseStub.getTranscriptionStub();
        TranscriptionStatusEntity approvedTranscriptionStatus = transcriptionStub.getTranscriptionStatusByEnum(APPROVED);

        TranscriptionWorkflowEntity approvedTranscriptionWorkflowEntity = transcriptionStub.createTranscriptionWorkflowEntity(
            transcriptionEntity,
            transcriptionEntity.getLastModifiedBy(),
            transcriptionEntity.getCreatedDateTime().plusHours(1),
            approvedTranscriptionStatus
        );

        assertEquals(0, dartsDatabaseStub.getTranscriptionCommentRepository().findAll().size());
        transcriptionEntity.getTranscriptionWorkflowEntities().add(approvedTranscriptionWorkflowEntity);
        transcriptionEntity.setTranscriptionStatus(approvedTranscriptionStatus);

        //transcriptionEntity.setTranscriptionDocument(createTranscriptionDocument());

        dartsDatabaseStub.getTranscriptionRepository().save(transcriptionEntity);

        assertEquals(APPROVED.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(3, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getEmailAddress()).thenReturn(testUser.getEmailAddress());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        testUserId = testUser.getId();
    }

    @Test
    @Transactional
    void getTranscription() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/transcriptions/%d", transcriptionId)));

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile("tests/transcriptions/transcription/expectedResponse.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @Transactional
    void getTranscriptionNotFound() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/transcriptions/%d", -99)));

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile("tests/transcriptions/transcription/expectedResponseNotFound.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }
}
