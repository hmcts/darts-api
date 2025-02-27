package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    private AuditApi auditApi;

    private ApplyAdminActionComponent applyAdminActionComponent;

    private ObjectHiddenReasonEntity objectHiddenReasonEntity;
    private UserAccountEntity userAccountEntity;
    private OffsetDateTime someDateTime;

    @BeforeEach
    void setUp() {
        applyAdminActionComponent = new ApplyAdminActionComponent(userIdentity,
                                                                  currentTimeHelper,
                                                                  removeAdminActionComponent,
                                                                  mediaRepository,
                                                                  adminActionRepository,
                                                                  auditApi);
    }

    @Nested
    class ApplyAdminActionToAllVersionsTests {



        @BeforeEach
        void setUp() {
            objectHiddenReasonEntity = new ObjectHiddenReasonEntity();

            userAccountEntity = PersistableFactory.getUserAccountTestData().someMinimal();
            when(userIdentity.getUserAccount())
                .thenReturn(userAccountEntity);

            someDateTime = OffsetDateTime.parse("2025-01-01T00:00:00Z");
            when(currentTimeHelper.currentOffsetDateTime())
                .thenReturn(someDateTime);
        }

        @Test
        void shouldHideAndSetAdminActionOnTargetedMedia_whenTargetedMediaHasNoChronicleId() {
            // Given
            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1)
                .build()
                .getEntity();

            var adminActionProperty = new ApplyAdminActionComponent.AdminActionProperties("Some ticket reference",
                                                                                          "Some comments",
                                                                                          objectHiddenReasonEntity);

            // When
            List<MediaEntity> mediaEntities = applyAdminActionComponent.applyAdminActionToAllVersions(targetedMedia, adminActionProperty);

            // Then
            assertEquals(1, mediaEntities.size());
            MediaEntity updatedMedia = mediaEntities.getFirst();

            assertTrue(updatedMedia.isHidden());

            Optional<ObjectAdminActionEntity> adminActionOptional = updatedMedia.getObjectAdminAction();
            assertTrue(adminActionOptional.isPresent());
            ObjectAdminActionEntity adminAction = adminActionOptional.get();

            assertEquals("Some ticket reference", adminAction.getTicketReference());
            assertEquals("Some comments", adminAction.getComments());
            assertEquals(targetedMedia.getId(), adminAction.getMedia().getId());
            assertEquals(objectHiddenReasonEntity, adminAction.getObjectHiddenReason());
            assertEquals(userAccountEntity, adminAction.getHiddenBy());
            assertEquals(someDateTime, adminAction.getHiddenDateTime());
            assertFalse(adminAction.isMarkedForManualDeletion());

            verify(removeAdminActionComponent).removeAdminActionFrom(List.of(targetedMedia));
            verifyNoMoreInteractions(removeAdminActionComponent);

            verify(adminActionRepository).saveAndFlush(adminAction);
            verifyNoMoreInteractions(adminActionRepository);

            verify(mediaRepository).saveAndFlush(updatedMedia);
            verifyNoMoreInteractions(mediaRepository);

            verify(auditApi).record(AuditActivity.HIDE_AUDIO, "Media id: 1, Ticket ref: Some ticket reference");
            verifyNoMoreInteractions(auditApi);
        }

        @Test
        void shouldHideAndSetAdminActionForAllVersions_whenTargetedMediaHasOtherVersions() {
            // Given
            final String commonChronicleId = "1000";

            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1)
                .chronicleId(commonChronicleId)
                .build()
                .getEntity();
            MediaEntity otherVersion = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(2)
                .chronicleId(commonChronicleId)
                .build()
                .getEntity();
            when(mediaRepository.findAllByChronicleId(commonChronicleId))
                .thenReturn(List.of(targetedMedia, otherVersion));

            var adminActionProperty = new ApplyAdminActionComponent.AdminActionProperties("Some ticket reference",
                                                                                          "Some comments",
                                                                                          objectHiddenReasonEntity);

            // When
            List<MediaEntity> mediaEntities = applyAdminActionComponent.applyAdminActionToAllVersions(targetedMedia, adminActionProperty);

            // Then
            assertEquals(2, mediaEntities.size());

            // Assert what we expect to be common across all versions
            for (MediaEntity updatedMedia : mediaEntities) {
                assertTrue(updatedMedia.isHidden());

                Optional<ObjectAdminActionEntity> adminActionOptional = updatedMedia.getObjectAdminAction();
                assertTrue(adminActionOptional.isPresent());
                ObjectAdminActionEntity adminAction = adminActionOptional.get();

                assertEquals("Some ticket reference", adminAction.getTicketReference());
                assertEquals("Some comments", adminAction.getComments());
                assertEquals(objectHiddenReasonEntity, adminAction.getObjectHiddenReason());
                assertEquals(userAccountEntity, adminAction.getHiddenBy());
                assertEquals(someDateTime, adminAction.getHiddenDateTime());
                assertFalse(adminAction.isMarkedForManualDeletion());

                verify(adminActionRepository).saveAndFlush(adminAction);
                verify(mediaRepository).saveAndFlush(updatedMedia);
            }
            verifyNoMoreInteractions(adminActionRepository);
            verifyNoMoreInteractions(mediaRepository);

            verify(removeAdminActionComponent).removeAdminActionFrom(List.of(targetedMedia, otherVersion));
            verifyNoMoreInteractions(removeAdminActionComponent);

            verify(auditApi).record(AuditActivity.HIDE_AUDIO, "Media id: 1, Ticket ref: Some ticket reference");
            verify(auditApi).record(AuditActivity.HIDE_AUDIO, "Media id: 2, Ticket ref: Some ticket reference");
            verifyNoMoreInteractions(auditApi);

            // And verify the back-links
            List<Integer> backLinkedMediaIds = mediaEntities.stream()
                .map(MediaEntity::getObjectAdminActions)
                .flatMap(List::stream)
                .map(ObjectAdminActionEntity::getMedia)
                .map(MediaEntity::getId)
                .toList();
            assertThat(backLinkedMediaIds, containsInAnyOrder(targetedMedia.getId(), otherVersion.getId()));
        }
    }

    @Nested
    class ApplyAdminActionToTests {

        @BeforeEach
        void setUp() {
            objectHiddenReasonEntity = new ObjectHiddenReasonEntity();

            userAccountEntity = PersistableFactory.getUserAccountTestData().someMinimal();
            when(userIdentity.getUserAccount())
                .thenReturn(userAccountEntity);

            someDateTime = OffsetDateTime.parse("2025-01-01T00:00:00Z");
            when(currentTimeHelper.currentOffsetDateTime())
                .thenReturn(someDateTime);
        }

        @Test
        void shouldHideAndSetAdminActionForAllProvidedMedias() {
            // Given
            final String commonChronicleId = "1000";

            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1)
                .chronicleId(commonChronicleId)
                .build()
                .getEntity();
            MediaEntity otherVersion = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(2)
                .chronicleId(commonChronicleId)
                .build()
                .getEntity();

            var adminActionProperty = new ApplyAdminActionComponent.AdminActionProperties("Some ticket reference",
                                                                                          "Some comments",
                                                                                          objectHiddenReasonEntity);

            // When
            List<MediaEntity> mediaEntities = applyAdminActionComponent.applyAdminActionTo(List.of(targetedMedia, otherVersion),
                                                                                           adminActionProperty);
            // Then
            assertEquals(2, mediaEntities.size());

            // Assert what we expect to be common across all versions
            for (MediaEntity updatedMedia : mediaEntities) {
                assertTrue(updatedMedia.isHidden());

                Optional<ObjectAdminActionEntity> adminActionOptional = updatedMedia.getObjectAdminAction();
                assertTrue(adminActionOptional.isPresent());
                ObjectAdminActionEntity adminAction = adminActionOptional.get();

                assertEquals("Some ticket reference", adminAction.getTicketReference());
                assertEquals("Some comments", adminAction.getComments());
                assertEquals(objectHiddenReasonEntity, adminAction.getObjectHiddenReason());
                assertEquals(userAccountEntity, adminAction.getHiddenBy());
                assertEquals(someDateTime, adminAction.getHiddenDateTime());
                assertFalse(adminAction.isMarkedForManualDeletion());

                verify(adminActionRepository).saveAndFlush(adminAction);
                verify(mediaRepository).saveAndFlush(updatedMedia);
            }
            verifyNoMoreInteractions(adminActionRepository);
            verifyNoMoreInteractions(mediaRepository);

            verify(removeAdminActionComponent).removeAdminActionFrom(List.of(targetedMedia, otherVersion));
            verifyNoMoreInteractions(removeAdminActionComponent);

            verify(auditApi).record(AuditActivity.HIDE_AUDIO, "Media id: 1, Ticket ref: Some ticket reference");
            verify(auditApi).record(AuditActivity.HIDE_AUDIO, "Media id: 2, Ticket ref: Some ticket reference");
            verifyNoMoreInteractions(auditApi);

            // And verify the back-links
            List<Integer> backLinkedMediaIds = mediaEntities.stream()
                .map(MediaEntity::getObjectAdminActions)
                .flatMap(List::stream)
                .map(ObjectAdminActionEntity::getMedia)
                .map(MediaEntity::getId)
                .toList();
            assertThat(backLinkedMediaIds, containsInAnyOrder(targetedMedia.getId(), otherVersion.getId()));
        }
    }

}
