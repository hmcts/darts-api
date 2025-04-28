package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
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
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.HiddenReason;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        List<ObjectAdminActionEntity> objectAdminActionEntity = objectAdminActionRepository.findByMediaId(mediaEntity.getId());

        MediaHideResponse mediaResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), MediaHideResponse.class);

        // ensure that the database data is contained in the response
        assertEquals(documentEntity.getId(), mediaResponse.getId());
        assertEquals(documentEntity.isHidden(), mediaResponse.getIsHidden());
        assertEquals(documentEntity.isDeleted(), mediaResponse.getIsDeleted());
        assertEquals(objectAdminActionEntity.getFirst().getId(), mediaResponse.getAdminAction().getId());
        assertEquals(objectAdminActionEntity.getFirst().getComments(), mediaResponse.getAdminAction().getComments());
        assertEquals(objectAdminActionEntity.getFirst().getTicketReference(), mediaResponse.getAdminAction().getTicketReference());
        assertEquals(objectAdminActionEntity.getFirst().getObjectHiddenReason().getId(), mediaResponse.getAdminAction().getReasonId());
        assertFalse(objectAdminActionEntity.getFirst().isMarkedForManualDeletion());
        assertEquals(objectAdminActionEntity.getFirst().getHiddenBy().getId(), mediaResponse.getAdminAction().getHiddenById());
        assertEquals(objectAdminActionEntity.getFirst()
                         .getHiddenDateTime().truncatedTo(ChronoUnit.SECONDS),
                     mediaResponse.getAdminAction().getHiddenAt().truncatedTo(ChronoUnit.SECONDS));
        assertNull(objectAdminActionEntity.getFirst().getMarkedForManualDelBy());
        assertNull(mediaResponse.getAdminAction().getMarkedForManualDeletionById());
        assertNull(objectAdminActionEntity.getFirst().getMarkedForManualDelDateTime());
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
        assertTrue(objectAdminActionRepository.findByTranscriptionDocumentId(mediaEntity.getId()).isEmpty());

        // assert that the action data that existed before deletion is returned
        assertEquals(documentEntity.getId(), mediaHideResponse.getId());
        assertEquals(documentEntity.isHidden(), mediaHideResponse.getIsHidden());
        assertNull(mediaHideResponse.getAdminAction());
    }

    @Test
    void testTranscriptionDocumentShowForbidden() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.SUPER_USER);

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(true);

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

            .andExpect(status().isUnprocessableEntity())
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

            .andExpect(status().isUnprocessableEntity())
            .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.MEDIA_HIDE_ACTION_REASON_NOT_FOUND.getType(), problemResponse.getType());
    }

    @Test
    void postAdminHideMediaId_usingNullIsHidden_shouldBeRejected() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MediaEntity mediaEntity = mediaStub.createAndSaveMedia();
        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(null);

        // hide the media
        MvcResult response = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString()))
                                                 .header("Content-Type", "application/json")
                                                 .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String expectedResponse = """
            {
              "violations": [
                {
                  "field": "isHidden",
                  "message": "must not be null"
                }
              ],
              "type": "https://zalando.github.io/problem/constraint-violation",
              "status": 400,
              "title": "Constraint Violation"
            }""";
        String actualResponse = response.getResponse().getContentAsString();
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
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
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String content = showResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getType(), problemResponse.getType());
    }

    @Test
    void shouldHideTargetedMediaAndAllOtherVersions_whenTargetedMediaHasChronicleId() throws Exception {
        // Given
        MediaEntity originalTargetedMedia = PersistableFactory.getMediaTestData()
            .someMinimalBuilder()
            .chronicleId("1000")
            .isHidden(false)
            .build()
            .getEntity();
        MediaEntity originalOtherVersion = PersistableFactory.getMediaTestData()
            .someMinimalBuilder()
            .chronicleId("1000")
            .isHidden(false)
            .build()
            .getEntity();
        dartsPersistence.saveAll(originalTargetedMedia, originalOtherVersion);

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(HiddenReason.OTHER_HIDE.getId());
        adminActionRequest.setComments("some comment");
        adminActionRequest.setTicketReference("some ticket ref");

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setAdminAction(adminActionRequest);
        mediaHideRequest.setIsHidden(true);

        UserAccountEntity clientUser = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // When
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, originalTargetedMedia.getId().toString()))
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Then
        List<ObjectAdminActionEntity> adminActionsForTargetedMedia = objectAdminActionRepository.findAll().stream()
            .filter(adminAction -> adminAction.getMedia().getId().equals(originalTargetedMedia.getId()))
            .toList();
        assertEquals(1, adminActionsForTargetedMedia.size());
        ObjectAdminActionEntity adminActionEntity = adminActionsForTargetedMedia.getFirst();

        JSONAssert.assertEquals(
            """
                        {
                          "id": 0,
                          "is_hidden": true,
                          "is_deleted": false,
                          "admin_action": {
                            "id": 0,
                            "reason_id": 0,
                            "hidden_by_id": 15000,
                            "hidden_at": "",
                            "is_marked_for_manual_deletion": false,
                            "ticket_reference": "some ticket ref",
                            "comments": "some comment"
                          }
                        }
                """,
            mvcResult.getResponse().getContentAsString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", (actual, expected) -> objectStringEquals(originalTargetedMedia.getId(), actual)),
                new Customization("admin_action.id", (actual, expected) -> objectStringEquals(adminActionEntity.getId(), actual)),
                new Customization("admin_action.reason_id", (actual, expected) -> HiddenReason.OTHER_HIDE.getId().equals(actual)),
                new Customization("admin_action.hidden_by_id", (actual, expected) -> clientUser.getId().equals(actual)),
                new Customization("admin_action.hidden_at", (actual, expected) -> isIsoDateTimeString((String) actual))
            )
        );

        // And assert further DB state
        getTransactionalUtil().executeInTransaction(() -> {
            MediaEntity finalTargetedMedia = mediaRepository.findById(originalTargetedMedia.getId())
                .orElseThrow();
            assertTrue(finalTargetedMedia.isHidden());
            assertTrue(finalTargetedMedia.getObjectAdminAction().isPresent());

            ObjectAdminActionEntity adminAction = finalTargetedMedia.getObjectAdminAction().get();
            assertEquals(HiddenReason.OTHER_HIDE.getId(), adminAction.getObjectHiddenReason().getId());
            assertEquals(originalTargetedMedia.getId(), adminAction.getMedia().getId());
            assertEquals(clientUser.getId(), adminAction.getHiddenBy().getId());
            assertNotNull(adminAction.getHiddenDateTime());
            assertFalse(adminAction.isMarkedForManualDeletion());
            assertNull(adminAction.getMarkedForManualDelBy());
            assertEquals("some ticket ref", adminAction.getTicketReference());
            assertEquals("some comment", adminAction.getComments());
        });

        getTransactionalUtil().executeInTransaction(() -> {
            MediaEntity finalOtherVersion = mediaRepository.findById(originalOtherVersion.getId())
                .orElseThrow();
            assertTrue(finalOtherVersion.isHidden());
            assertTrue(finalOtherVersion.getObjectAdminAction().isPresent());

            ObjectAdminActionEntity adminAction = finalOtherVersion.getObjectAdminAction().get();
            assertEquals(HiddenReason.OTHER_HIDE.getId(), adminAction.getObjectHiddenReason().getId());
            assertEquals(originalOtherVersion.getId(), adminAction.getMedia().getId());
            assertEquals(clientUser.getId(), adminAction.getHiddenBy().getId());
            assertNotNull(adminAction.getHiddenDateTime());
            assertFalse(adminAction.isMarkedForManualDeletion());
            assertNull(adminAction.getMarkedForManualDelBy());
            assertEquals("some ticket ref", adminAction.getTicketReference());
            assertEquals("some comment", adminAction.getComments());
        });

        List<AuditEntity> hideAudio = dartsDatabase.getAuditRepository().findAll().stream()
            .filter(audit -> AuditActivity.HIDE_AUDIO.getId().equals(audit.getAuditActivity().getId()))
            .toList();
        assertEquals(2, hideAudio.size());
    }

    @Test
    void shouldUnHideTargetedMediaAndAllOtherVersions_whenTargetedMediaHasChronicleId() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MediaEntity originalTargetedMedia = PersistableFactory.getMediaTestData()
            .someMinimalBuilder()
            .chronicleId("1000")
            .isHidden(true)
            .build()
            .getEntity();
        MediaEntity originalOtherVersion = PersistableFactory.getMediaTestData()
            .someMinimalBuilder()
            .chronicleId("1000")
            .isHidden(true)
            .build()
            .getEntity();
        dartsPersistence.saveAll(originalTargetedMedia, originalOtherVersion);

        ObjectAdminActionEntity targetedMediaAction = PersistableFactory.getObjectAdminActionTestData()
            .someMinimalBuilder()
            .media(originalTargetedMedia)
            .build()
            .getEntity();
        ObjectAdminActionEntity otherVersionAction = PersistableFactory.getObjectAdminActionTestData()
            .someMinimalBuilder()
            .media(originalOtherVersion)
            .build()
            .getEntity();
        dartsPersistence.saveAll(targetedMediaAction, otherVersionAction);

        MediaHideRequest mediaHideRequest = new MediaHideRequest();
        mediaHideRequest.setIsHidden(false);

        // When
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                MEDIA_ID_SUBSTITUTION_KEY, originalTargetedMedia.getId().toString()))
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(mediaHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Then
        JSONAssert.assertEquals(
            """
                {
                  "id": 0,
                  "is_hidden": false,
                  "is_deleted": false
                }
                """,
            mvcResult.getResponse().getContentAsString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", (actual, expected) -> objectStringEquals(actual, originalTargetedMedia.getId()))
            )
        );

        // And assert DB state
        getTransactionalUtil().executeInTransaction(() -> {
            MediaEntity finalTargetedMedia = mediaRepository.findById(originalTargetedMedia.getId())
                .orElseThrow();
            assertFalse(finalTargetedMedia.isHidden());
            assertFalse(finalTargetedMedia.getObjectAdminAction().isPresent());
        });

        getTransactionalUtil().executeInTransaction(() -> {
            MediaEntity finalOtherVersion = mediaRepository.findById(originalOtherVersion.getId())
                .orElseThrow();
            assertFalse(finalOtherVersion.isHidden());
            assertFalse(finalOtherVersion.getObjectAdminAction().isPresent());
        });

        List<AuditEntity> hideAudio = dartsDatabase.getAuditRepository().findAll().stream()
            .filter(audit -> AuditActivity.UNHIDE_AUDIO.getId().equals(audit.getAuditActivity().getId()))
            .toList();
        assertEquals(2, hideAudio.size());
    }

}