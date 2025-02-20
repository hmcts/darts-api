package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyAdminActionComponentTest {

    @Mock
    private UserIdentity userIdentity;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private RemoveAdminActionComponent removeAdminActionComponent;

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private ObjectAdminActionRepository adminActionRepository;
    @Mock
    private ObjectHiddenReasonRepository hiddenReasonRepository;

    @Mock
    private AuditApi auditApi;

    private ApplyAdminActionComponent applyAdminActionComponent;

    private UserAccountEntity userAccountEntity;
    private ObjectHiddenReasonEntity objectHiddenReasonEntity;
    private OffsetDateTime someDateTime;

    @BeforeEach
    void setUp() {
        applyAdminActionComponent = new ApplyAdminActionComponent(userIdentity,
                                      currentTimeHelper,
                                      removeAdminActionComponent,
                                      mediaRepository,
                                      adminActionRepository,
                                      hiddenReasonRepository,
                                      auditApi);

        objectHiddenReasonEntity = new ObjectHiddenReasonEntity();
        when(hiddenReasonRepository.findById(0))
            .thenReturn(Optional.of(objectHiddenReasonEntity));

        userAccountEntity = PersistableFactory.getUserAccountTestData().someMinimal();
        when(userIdentity.getUserAccount())
            .thenReturn(PersistableFactory.getUserAccountTestData().someMinimal());

        someDateTime = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(someDateTime);
    }

    @Test
    public void applyAdminAction_shouldHideTargetedMediaAndSetAdminAction_whenTargetedMediaHasNoChronicleId() {
        // Given
        MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .build()
            .getEntity();

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(0);
        adminActionRequest.setComments("Some comments");
        adminActionRequest.setTicketReference("Some ticket reference");

        // When
        List<MediaEntity> mediaEntities = applyAdminActionComponent.applyAdminAction(targetedMedia, adminActionRequest);

        // Then
        assertEquals(1, mediaEntities.size());
        MediaEntity updatedMedia = mediaEntities.getFirst();

        assertTrue(updatedMedia.isHidden());

        List<ObjectAdminActionEntity> adminActions = updatedMedia.getObjectAdminActions();
        assertEquals(1, adminActions.size()); // TODO: Fails because adminActions does not get set explicitly in the MediaEntity
        ObjectAdminActionEntity adminAction = adminActions.getFirst();

        assertEquals("Some ticket reference", adminAction.getTicketReference());
        assertEquals("Some comments", adminAction.getComments());
        assertEquals(targetedMedia, adminAction.getMedia());
        assertEquals(objectHiddenReasonEntity, adminAction.getObjectHiddenReason());
        assertEquals(userAccountEntity, adminAction.getHiddenBy());
        assertEquals(someDateTime, adminAction.getHiddenDateTime());
        assertFalse(adminAction.isMarkedForManualDeletion());
    }

}