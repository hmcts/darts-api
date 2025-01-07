package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtroomStub;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;
import uk.gov.hmcts.darts.testutils.stubs.ObjectAdminActionStub;
import uk.gov.hmcts.darts.testutils.stubs.ObjectHiddenReasonStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.net.URI;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AudioControllerGetMarkedForDeletionIntTest extends PostgresIntegrationBase {

    private static final URI ENDPOINT = URI.create("/admin/medias/marked-for-deletion");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private UserAccountStub userAccountStub;

    @Autowired
    private CourtroomStub courtroomStub;

    @Autowired
    private MediaStub mediaStub;

    @Autowired
    private ObjectAdminActionStub objectAdminActionStub;

    @Autowired
    private ObjectHiddenReasonStub objectHiddenReasonStub;

    @Test
    void getMediasMarkedForDeletionShouldReturnExpectedResponseWhenNoMediaIsMarkedForDeletion() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT)
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        JSONAssert.assertEquals("[]",
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediasMarkedForDeletionShouldReturnExpectedResponseWhenMediaExistsWithDeletionReasonButNotYetApprovedForDeletion() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var courtroomEntity = courtroomStub.createCourtroomUnlessExists("Test Courthouse", "Test Courtroom",
                                                                        userAccountStub.getSystemUserAccountEntity());

        // And a media that's marked for deletion, but not yet approved for deletion (not marked for manual deletion)
        var expectedMediaEntity = createAndSaveMediaEntity(courtroomEntity);
        var expectedObjectAdminActionEntity = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                                                      .media(expectedMediaEntity)
                                                                                      .objectHiddenReason(
                                                                                          objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                                                      .markedForManualDeletion(false)
                                                                                      .markedForManualDelBy(null)
                                                                                      .markedForManualDelDateTime(null)
                                                                                      .build());

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT)
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    [
                                      {
                                        "media_id": %d,
                                        "channel": 1,
                                        "start_at": "2024-01-01T00:00:00Z",
                                        "end_at": "2024-01-01T00:00:00Z",
                                        "courthouse": {
                                          "id": 1,
                                          "display_name": "TEST COURTHOUSE"
                                        },
                                        "courtroom": {
                                          "id": 1,
                                          "name": "TEST COURTROOM"
                                        },
                                        "admin_action": {
                                          "id": %d,
                                          "reason_id": 1,
                                          "hidden_by_id": 0,
                                          "hidden_at": "2024-01-01T00:00:00Z",
                                          "is_marked_for_manual_deletion": false,
                                          "ticket_reference": "Some ticket reference",
                                          "comments": "Some comment"
                                        }
                                      }
                                    ]""".formatted(expectedMediaEntity.getId(),
                                                   expectedObjectAdminActionEntity.getId()),
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediasMarkedForDeletionShouldFailWhenUserIsNotAuthorised() throws Exception {
        // Given
        superAdminUserStub.givenUserIsNotAuthorised(userIdentity);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT)
                    .contentType("application/json"))
            .andExpect(status().isForbidden())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                      "type": "AUTHORISATION_109",
                                      "title": "User is not authorised for this endpoint",
                                      "status": 403
                                    }
                                    """,
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE);
    }

    private MediaEntity createAndSaveMediaEntity(CourtroomEntity courtroomEntity) {
        return mediaStub.createMediaEntity(courtroomEntity.getCourthouse().getCourthouseName(),
                                           courtroomEntity.getName(),
                                           OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                                           OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                                           1,
                                           "MP2");
    }

}
