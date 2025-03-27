package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AdminActionResponse;
import uk.gov.hmcts.darts.audio.model.MediaApproveMarkedForDeletionResponse;
import uk.gov.hmcts.darts.audio.model.Problem;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity_;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtroomStub;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;
import uk.gov.hmcts.darts.testutils.stubs.ObjectAdminActionStub;
import uk.gov.hmcts.darts.testutils.stubs.ObjectHiddenReasonStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static wiremock.org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@AutoConfigureMockMvc
class AudioControllerPostAdminApproveMediaMarkedForDeletionIntTest extends IntegrationBase {
    private static final String MEDIA_ID_SUBSTITUTION_KEY = "{media_id}";
    private static final String ENDPOINT_URL = "/admin/medias/" + MEDIA_ID_SUBSTITUTION_KEY + "/approve-deletion";
    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2024-01-01T10:00:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2024-01-01T00:12:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    UserIdentity userIdentity;

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

    private MediaEntity mediaEntity;

    private String endpoint;

    @BeforeEach
    void setUp() {
        String courtHouseName = randomAlphanumeric(8);
        var courtroomEntity = courtroomStub.createCourtroomUnlessExists(courtHouseName, "Test Courtroom",
                                                                        userAccountStub.getSystemUserAccountEntity());

        // And a media that's marked for deletion, but not yet approved for deletion (not marked for manual deletion)
        mediaEntity = createAndSaveMediaEntity(courtroomEntity);

        endpoint = ENDPOINT_URL.replace(MEDIA_ID_SUBSTITUTION_KEY, mediaEntity.getId().toString());
    }

    @Test
    void postAdminApproveMediaMarkedForDeletionShouldReturnSuccess() throws Exception {
        // given
        var superAdminUser = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity("testuser");
        ObjectAdminActionEntity adminActionEntity = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                                                            .media(mediaEntity)
                                                                                            .objectHiddenReason(
                                                                                                objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                                                            .markedForManualDeletion(false)
                                                                                            .markedForManualDelBy(null)
                                                                                            .markedForManualDelDateTime(null)
                                                                                            .hiddenBy(testUser)
                                                                                            .build());

        // when
        MvcResult mvcResult = mockMvc.perform(post(endpoint))
            .andExpect(status().isOk())
            .andReturn();

        assertAudit(adminActionEntity);

        // then
        MediaApproveMarkedForDeletionResponse mediaApproveMarkedForDeletionResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), MediaApproveMarkedForDeletionResponse.class);
        AdminActionResponse actionResponse = mediaApproveMarkedForDeletionResponse.getAdminAction();
        assertNotNull(actionResponse.getMarkedForManualDeletionAt());
        assertEquals(superAdminUser.getId(), actionResponse.getMarkedForManualDeletionById());
        assertTrue(actionResponse.getIsMarkedForManualDeletion());
    }

    @Test
    void postAdminApproveMediaMarkedForDeletionWhereCurrentUserMarkedForDeletion() throws Exception {
        // given
        var superAdminUser = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .media(mediaEntity)
                                                .objectHiddenReason(
                                                    objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                .markedForManualDeletion(false)
                                                .markedForManualDelBy(null)
                                                .markedForManualDelDateTime(null)
                                                .hiddenBy(superAdminUser)
                                                .build());

        // when
        MvcResult mvcResult = mockMvc.perform(post(endpoint))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(problem.getType(), AudioApiError.USER_CANNOT_APPROVE_THEIR_OWN_DELETION.getType());
        assertEquals(problem.getTitle(), AudioApiError.USER_CANNOT_APPROVE_THEIR_OWN_DELETION.getTitle());
    }

    @Test
    void postAdminApproveMediaMarkedForDeletionWhereMediaMarkedForDeletionTwice() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity("testuser");

        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .media(mediaEntity)
                                                .objectHiddenReason(
                                                    objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                .markedForManualDeletion(false)
                                                .markedForManualDelBy(null)
                                                .markedForManualDelDateTime(null)
                                                .hiddenBy(testUser)
                                                .build());

        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .media(mediaEntity)
                                                .objectHiddenReason(
                                                    objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                .markedForManualDeletion(false)
                                                .markedForManualDelBy(null)
                                                .markedForManualDelDateTime(null)
                                                .hiddenBy(testUser)
                                                .build());

        // when
        MvcResult mvcResult = mockMvc.perform(post(endpoint))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(problem.getType(), AudioApiError.TOO_MANY_RESULTS.getType());
        assertEquals(problem.getTitle(), AudioApiError.TOO_MANY_RESULTS.getTitle());
    }

    @Test
    void postAdminApproveMediaMarkedForDeletionWhereMediaNotMarkedForDeletion() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity("testuser");

        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .media(mediaEntity)
                                                .objectHiddenReason(
                                                    objectHiddenReasonStub.getAnyWithMarkedForDeletion(false))
                                                .markedForManualDeletion(false)
                                                .markedForManualDelBy(null)
                                                .markedForManualDelDateTime(null)
                                                .hiddenBy(testUser)
                                                .build());

        // when
        MvcResult mvcResult = mockMvc.perform(post(endpoint))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(problem.getType(), AudioApiError.MEDIA_MARKED_FOR_DELETION_REASON_NOT_FOUND.getType());
        assertEquals(problem.getTitle(), AudioApiError.MEDIA_MARKED_FOR_DELETION_REASON_NOT_FOUND.getTitle());
    }

    @Test
    void postAdminApproveMediaMarkedForDeletionAlreadyApproved() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity("testuser");
        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .media(mediaEntity)
                                                .objectHiddenReason(
                                                    objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                .hiddenBy(testUser)
                                                .build());

        // when
        MvcResult mvcResult = mockMvc.perform(post(endpoint))
            .andExpect(status().isConflict())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(problem.getType(), AudioApiError.MEDIA_ALREADY_MARKED_FOR_DELETION.getType());
        assertEquals(problem.getTitle(), AudioApiError.MEDIA_ALREADY_MARKED_FOR_DELETION.getTitle());

    }

    @Test
    void postAdminApproveMediaMarkedForDeletionMediaNotMarkedForDeletion() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // when
        MvcResult mvcResult = mockMvc.perform(post(endpoint))
            .andExpect(status().isNotFound())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(problem.getType(), AudioApiError.ADMIN_MEDIA_MARKED_FOR_DELETION_NOT_FOUND.getType());
        assertEquals(problem.getTitle(), AudioApiError.ADMIN_MEDIA_MARKED_FOR_DELETION_NOT_FOUND.getTitle());

    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void testForbidden(SecurityRoleEnum role) throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(userIdentity, role);

        // when then
        mockMvc.perform(post(endpoint))
            .andExpect(status().isForbidden())
            .andReturn();

    }

    private MediaEntity createAndSaveMediaEntity(CourtroomEntity courtroomEntity) {
        return mediaStub.createMediaEntity(courtroomEntity.getCourthouse().getCourthouseName(),
                                           courtroomEntity.getName(),
                                           START_TIME,
                                           END_TIME,
                                           1,
                                           "MP2");
    }

    private void assertAudit(ObjectAdminActionEntity objectAdminActionEntity) {
        List<AuditEntity> caseExpiredAuditEntries = dartsDatabase.getAuditRepository()
            .findAll((Specification<AuditEntity>) (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get(AuditEntity_.additionalData), String.valueOf(objectAdminActionEntity.getId())),
                criteriaBuilder.equal(root.get(AuditEntity_.auditActivity).get("id"), AuditActivity.MANUAL_DELETION.getId())
            ));

        // assert additional audit data
        assertFalse(caseExpiredAuditEntries.isEmpty());
        assertEquals(1, caseExpiredAuditEntries.size());
        assertNotNull(caseExpiredAuditEntries.getFirst().getCreatedBy());
        assertNotNull(caseExpiredAuditEntries.getFirst().getLastModifiedBy());
        assertNotNull(caseExpiredAuditEntries.getFirst().getCreatedDateTime());
        assertNotNull(caseExpiredAuditEntries.getFirst().getLastModifiedDateTime());
        assertEquals(userIdentity.getUserAccount().getId(), caseExpiredAuditEntries.getFirst().getUser().getId());
        assertNull(caseExpiredAuditEntries.getFirst().getCourtCase());
    }

}