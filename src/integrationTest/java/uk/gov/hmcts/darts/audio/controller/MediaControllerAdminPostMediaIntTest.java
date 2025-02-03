package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideResponse;
import uk.gov.hmcts.darts.audio.model.Problem;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.enums.HiddenReason;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MediaControllerAdminPostMediaIntTest extends IntegrationBase {

    private static final String MEDIA_ID_SUBSTITUTION_KEY = "${MEDIA_ID}";
    private static final String ENDPOINT_URL = "/admin/medias/" + MEDIA_ID_SUBSTITUTION_KEY + "/hide";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaStub mediaStub;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ObjectAdminActionRepository objectAdminActionRepository;

    @ParameterizedTest
    @EnumSource(value = HiddenReason.class, names = {"OTHER_HIDE", "OTHER_DELETE"})
    void testMediaDocumentHideSuccess(HiddenReason hiddenReason) throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MediaEntity mediaEntity = mediaStub
            .createAndSaveMedia();

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        String comment = "comments";
        String ticketReference = "reference";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(hiddenReason.getId());
        adminActionRequest.setComments(comment);
        adminActionRequest.setTicketReference(ticketReference);
        mediaHideRequest.setAdminAction(adminActionRequest);

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        MediaEntity documentEntity = mediaRepository.findById(mediaEntity.getId()).get();
        List<ObjectAdminActionEntity> objectAdminActionEntity = objectAdminActionRepository.findByMedia_Id(mediaEntity.getId());

        MediaHideResponse mediaResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), MediaHideResponse.class);

        // ensure that the database data is contained in the response
        assertEquals(documentEntity.getId(), mediaResponse.getId());
        assertEquals(documentEntity.isHidden(), mediaResponse.getIsHidden());
        assertEquals(documentEntity.isDeleted(), mediaResponse.getIsDeleted());
        assertEquals(objectAdminActionEntity.get(0).getId(), mediaResponse.getAdminAction().getId());
        assertEquals(objectAdminActionEntity.get(0).getComments(), mediaResponse.getAdminAction().getComments());
        assertEquals(objectAdminActionEntity.get(0).getTicketReference(), mediaResponse.getAdminAction().getTicketReference());
        assertEquals(objectAdminActionEntity.get(0).getObjectHiddenReason().getId(), mediaResponse.getAdminAction().getReasonId());
        assertFalse(objectAdminActionEntity.get(0).isMarkedForManualDeletion());
        assertEquals(objectAdminActionEntity.get(0).getHiddenBy().getId(), mediaResponse.getAdminAction().getHiddenById());
        assertEquals(objectAdminActionEntity.get(0)
                         .getHiddenDateTime().truncatedTo(ChronoUnit.SECONDS),
                     mediaResponse.getAdminAction().getHiddenAt().truncatedTo(ChronoUnit.SECONDS));
        assertNull(objectAdminActionEntity.get(0).getMarkedForManualDelBy());
        assertNull(mediaResponse.getAdminAction().getMarkedForManualDeletionById());
        assertNull(objectAdminActionEntity.get(0).getMarkedForManualDelDateTime());
        assertNull(mediaResponse.getAdminAction().getMarkedForManualDeletionAt());
    }

    @Test
    void testMediaDocumentHideTwiceFailure() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MediaEntity mediaEntity = mediaStub.createAndSaveMedia();

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        String comment = "comments";
        String ticketReference = "reference";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(HiddenReason.OTHER_HIDE.getId());
        adminActionRequest.setComments(comment);
        adminActionRequest.setTicketReference(ticketReference);
        mediaHideRequest.setAdminAction(adminActionRequest);

        // run the test
        mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        MvcResult hideSecondCall = mockMvc.perform(post(ENDPOINT_URL.replace(
                "${MEDIA_ID}", mediaEntity.getId().toString()))
                                                       .header("Content-Type", "application/json")
                                                       .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().isConflict())
            .andReturn();

        String content = hideSecondCall.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.MEDIA_ALREADY_HIDDEN.getType(), problemResponse.getType());
    }

    @Test
    void testMediaDocumentShowSuccess() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MediaEntity mediaEntity = mediaStub.createAndSaveMedia();
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        String comment = "comments";
        String ticketReference = "reference";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(HiddenReason.OTHER_HIDE.getId());
        adminActionRequest.setComments(comment);
        adminActionRequest.setTicketReference(ticketReference);
        mediaHideRequest.setAdminAction(adminActionRequest);

        // hide the media
        mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        mediaHideRequest.setAdminAction(null);
        mediaHideRequest.setIsHidden(false);

        // now show the media
        MvcResult showResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                                                   .header("Content-Type", "application/json")
                                                   .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // a follow up show even if already shown will not error
        mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // make the assertions against the response
        MediaHideResponse mediaHideResponse
            = objectMapper.readValue(showResult.getResponse().getContentAsByteArray(), MediaHideResponse.class);
        MediaEntity documentEntity = mediaRepository.findById(mediaEntity.getId()).get();

        // ensure no object admin actions exist
        assertTrue(objectAdminActionRepository.findByTranscriptionDocument_Id(mediaEntity.getId()).isEmpty());

        // assert that the action data that existed before deletion is returned
        assertEquals(documentEntity.getId(), mediaHideResponse.getId());
        assertEquals(documentEntity.isHidden(), mediaHideResponse.getIsHidden());
        assertNull(mediaHideResponse.getAdminAction());
    }

    @Test
    void testTranscriptionDocumentShowForbidden() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.SUPER_USER);

        MediaHideRequest mediaHideRequest = new MediaHideRequest();

        mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, Integer.valueOf(-12).toString()))
                            .content(objectMapper.writeValueAsString(mediaHideRequest))
                            .header("Content-Type", "application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void testMediaDocumentIdDoesntExist() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.SUPER_ADMIN);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);


        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, Integer.valueOf(-12).toString()))
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().isNotFound())
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.MEDIA_NOT_FOUND.getType(), problemResponse.getType());
    }

    @Test
    void testMediaDocumentIdHideNoAdminAction() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MediaEntity mediaEntity = mediaStub.createAndSaveMedia();

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                                                  .content(objectMapper.writeValueAsString(mediaHideRequest))
                                                  .header("Content-Type", "application/json"))

            .andExpect(status().isBadRequest())
            .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE.getType(), problemResponse.getType());
    }

    @Test
    void testMediaIdHideAdminActionWithIncorrectReason() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MediaEntity mediaEntity = mediaStub.createAndSaveMedia();

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        Integer invalidReasonId = -121;
        adminActionRequest.setReasonId(invalidReasonId);
        adminActionRequest.setComments("");
        adminActionRequest.setTicketReference("");
        mediaHideRequest.setAdminAction(adminActionRequest);


        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                                                  .content(objectMapper.writeValueAsString(mediaHideRequest))
                                                  .header("Content-Type", "application/json"))

            .andExpect(status().isBadRequest())
            .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.MEDIA_HIDE_ACTION_REASON_NOT_FOUND.getType(), problemResponse.getType());
    }

    @Test
    void testMediaShowWithAdminActionFailure() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MediaEntity mediaEntity = mediaStub.createAndSaveMedia();
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

        String comment = "comments";
        String ticketReference = "reference";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(HiddenReason.OTHER_HIDE.getId());
        adminActionRequest.setComments(comment);
        adminActionRequest.setTicketReference(ticketReference);
        mediaHideRequest.setAdminAction(adminActionRequest);

        // hide the media
        mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        mediaHideRequest.setIsHidden(false);

        // now show the media
        MvcResult showResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                                                   .header("Content-Type", "application/json")
                                                   .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().isBadRequest())
            .andReturn();

        String content = showResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getType(), problemResponse.getType());
    }
}