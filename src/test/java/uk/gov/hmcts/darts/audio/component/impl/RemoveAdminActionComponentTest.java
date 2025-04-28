package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.test.common.data.MediaTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveAdminActionComponentTest {

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private ObjectAdminActionRepository adminActionRepository;

    @Mock
    private AuditApi auditApi;

    private RemoveAdminActionComponent removeAdminActionComponent;

    @BeforeEach
    void setUp() {
        removeAdminActionComponent = new RemoveAdminActionComponent(mediaRepository,
                                                                    adminActionRepository,
                                                                    auditApi);
    }

    @Nested
    class RemoveAdminActionFromAllVersionsTests {

        @Test
        void shouldUnHideTargetedMediaAndRemoveAdminAction_whenTargetedMediaIsHiddenAndHasNoChronicleId() {
            // Given
            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1L)
                .chronicleId(null)
                .isHidden(true)
                .build()
                .getEntity();

            ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
            objectAdminActionEntity.setId(100);
            objectAdminActionEntity.setTicketReference("Some ticket reference");
            objectAdminActionEntity.setComments("Some comments");
            targetedMedia.setObjectAdminAction(objectAdminActionEntity);

            // When
            List<MediaEntity> mediaEntities = removeAdminActionComponent.removeAdminActionFromAllVersions(targetedMedia);

            // Then
            assertEquals(1, mediaEntities.size());
            MediaEntity mediaEntity = mediaEntities.getFirst();
            assertEquals(1, mediaEntity.getId());
            assertFalse(mediaEntity.isHidden());
            assertFalse(mediaEntity.getObjectAdminAction().isPresent());

            verify(adminActionRepository).deleteById(eq(100));
            verifyNoMoreInteractions(adminActionRepository);

            verify(auditApi).record(AuditActivity.UNHIDE_AUDIO, "Media id: 1, Ticket ref: Some ticket reference, Comments: Some comments");
            verifyNoMoreInteractions(auditApi);

            verify(mediaRepository).saveAndFlush(argThat(media -> media.getId().equals(1L)));
            verifyNoMoreInteractions(mediaRepository);
        }

        @Test
        void shouldUnHideAndSetAdminActionForAllVersions_whenTargetedMediaIsHiddenAndHasOtherVersions() {
            // Given
            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1L)
                .chronicleId("1000")
                .isHidden(true)
                .build()
                .getEntity();

            ObjectAdminActionEntity adminActionForTargetedMedia = new ObjectAdminActionEntity();
            adminActionForTargetedMedia.setId(100);
            adminActionForTargetedMedia.setTicketReference("Some ticket reference");
            adminActionForTargetedMedia.setComments("Some comments");
            targetedMedia.setObjectAdminAction(adminActionForTargetedMedia);

            MediaEntity otherVersion = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(2L)
                .chronicleId("1000")
                .isHidden(true)
                .build()
                .getEntity();

            ObjectAdminActionEntity adminActionForOtherVersion = new ObjectAdminActionEntity();
            adminActionForOtherVersion.setId(200);
            adminActionForOtherVersion.setTicketReference("Some ticket reference");
            adminActionForOtherVersion.setComments("Some comments");
            otherVersion.setObjectAdminAction(adminActionForOtherVersion);

            when(mediaRepository.findAllByChronicleId("1000")).thenReturn(List.of(targetedMedia, otherVersion));

            // When
            List<MediaEntity> mediaEntities = removeAdminActionComponent.removeAdminActionFromAllVersions(targetedMedia);

            // Then
            assertEquals(2, mediaEntities.size());
            {
                MediaEntity mediaEntity = mediaEntities.getFirst();
                assertEquals(1, mediaEntity.getId());
                assertFalse(mediaEntity.isHidden());
                assertFalse(mediaEntity.getObjectAdminAction().isPresent());
            }
            {
                MediaEntity mediaEntity = mediaEntities.get(1);
                assertEquals(2, mediaEntity.getId());
                assertFalse(mediaEntity.isHidden());
                assertFalse(mediaEntity.getObjectAdminAction().isPresent());
            }

            verify(adminActionRepository).deleteById(eq(100));
            verify(adminActionRepository).deleteById(eq(200));
            verifyNoMoreInteractions(adminActionRepository);

            verify(auditApi).record(AuditActivity.UNHIDE_AUDIO, "Media id: 1, Ticket ref: Some ticket reference, Comments: Some comments");
            verify(auditApi).record(AuditActivity.UNHIDE_AUDIO, "Media id: 2, Ticket ref: Some ticket reference, Comments: Some comments");
            verifyNoMoreInteractions(auditApi);

            verify(mediaRepository).saveAndFlush(argThat(media -> media.getId().equals(1L)));
            verify(mediaRepository).saveAndFlush(argThat(media -> media.getId().equals(2L)));
            verifyNoMoreInteractions(mediaRepository);
        }

    }

    @Nested
    class RemoveAdminActionFromTests {

        @Test
        void shouldUnHideTargetedMediaAndRemoveAdminAction_whenTargetedMediaIsHiddenAndHasExistingAdminActionAndHasNoChronicleId() {
            // Given
            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1L)
                .chronicleId(null)
                .isHidden(true)
                .build()
                .getEntity();

            ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
            objectAdminActionEntity.setId(100);
            objectAdminActionEntity.setTicketReference("Some ticket reference");
            objectAdminActionEntity.setComments("Some comments");
            targetedMedia.setObjectAdminAction(objectAdminActionEntity);

            // When
            List<MediaEntity> mediaEntities = removeAdminActionComponent.removeAdminActionFrom(Collections.singletonList(targetedMedia));

            // Then
            assertEquals(1, mediaEntities.size());
            MediaEntity mediaEntity = mediaEntities.getFirst();
            assertEquals(1L, mediaEntity.getId());
            assertFalse(mediaEntity.isHidden());
            assertFalse(mediaEntity.getObjectAdminAction().isPresent());

            verify(adminActionRepository).deleteById(eq(100));
            verifyNoMoreInteractions(adminActionRepository);

            verify(auditApi).record(AuditActivity.UNHIDE_AUDIO, "Media id: 1, Ticket ref: Some ticket reference, Comments: Some comments");
            verifyNoMoreInteractions(auditApi);

            verify(mediaRepository).saveAndFlush(argThat(media -> media.getId().equals(1L)));
            verifyNoMoreInteractions(mediaRepository);
        }

        @Test
        void shouldUnHideTargetedMedia_whenTargetedMediaIsHiddenAndHasNoExistingAdminAction() {
            // Given
            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1L)
                .chronicleId("1000")
                .isHidden(true)
                .build()
                .getEntity();

            // When
            List<MediaEntity> mediaEntities = removeAdminActionComponent.removeAdminActionFrom(Collections.singletonList(targetedMedia));

            // Then
            assertEquals(1, mediaEntities.size());
            MediaEntity mediaEntity = mediaEntities.getFirst();
            assertEquals(1L, mediaEntity.getId());
            assertFalse(mediaEntity.isHidden());
            assertFalse(mediaEntity.getObjectAdminAction().isPresent());

            verifyNoInteractions(adminActionRepository);

            verify(auditApi).record(AuditActivity.UNHIDE_AUDIO, "Media id: 1, Ticket ref: null, Comments: null");
            verifyNoMoreInteractions(auditApi);

            verify(mediaRepository).saveAndFlush(argThat(media -> media.getId().equals(1L)));
            verifyNoMoreInteractions(mediaRepository);
        }

        @Test
        void shouldUnHideTargetedMedia_whenTargetedMediaIsNotHiddenAndHasExistingAdminAction() {
            // Given
            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1L)
                .chronicleId("1000")
                .isHidden(false)
                .build()
                .getEntity();

            ObjectAdminActionEntity adminActionForTargetedMedia = new ObjectAdminActionEntity();
            adminActionForTargetedMedia.setId(100);
            adminActionForTargetedMedia.setTicketReference("Some ticket reference");
            adminActionForTargetedMedia.setComments("Some comments");
            targetedMedia.setObjectAdminAction(adminActionForTargetedMedia);

            // When
            List<MediaEntity> mediaEntities = removeAdminActionComponent.removeAdminActionFrom(Collections.singletonList(targetedMedia));

            // Then
            assertEquals(1, mediaEntities.size());
            MediaEntity mediaEntity = mediaEntities.getFirst();
            assertEquals(1L, mediaEntity.getId());
            assertFalse(mediaEntity.isHidden());
            assertFalse(mediaEntity.getObjectAdminAction().isPresent());

            verify(adminActionRepository).deleteById(eq(100));
            verifyNoMoreInteractions(adminActionRepository);

            verify(auditApi).record(AuditActivity.UNHIDE_AUDIO, "Media id: 1, Ticket ref: Some ticket reference, Comments: Some comments");
            verifyNoMoreInteractions(auditApi);

            verify(mediaRepository).saveAndFlush(argThat(media -> media.getId().equals(1L)));
            verifyNoMoreInteractions(mediaRepository);
        }

        @Test
        void shouldNotUnHideTargetedMedia_whenTargetedMediaIsNotHiddenAndHasNoExistingAdminAction() {
            // Given
            MediaEntity targetedMedia = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .id(1L)
                .chronicleId("1000")
                .isHidden(false)
                .build()
                .getEntity();

            // When
            List<MediaEntity> mediaEntities = removeAdminActionComponent.removeAdminActionFrom(Collections.singletonList(targetedMedia));

            // Then
            assertEquals(1, mediaEntities.size());
            MediaEntity mediaEntity = mediaEntities.getFirst();
            assertEquals(1, mediaEntity.getId());
            assertFalse(mediaEntity.isHidden());
            assertFalse(mediaEntity.getObjectAdminAction().isPresent());

            verifyNoInteractions(adminActionRepository);
            verifyNoInteractions(auditApi);
            verifyNoInteractions(mediaRepository);
        }

        @Test
        void shouldThrowException_whenMediasHaveDifferingChronicleIds() {
            // Given
            MediaTestData mediaTestData = PersistableFactory.getMediaTestData();

            MediaEntity media1 = mediaTestData.someMinimalBuilder()
                .id(1L)
                .chronicleId("1000")
                .build()
                .getEntity();
            MediaEntity media2 = mediaTestData.someMinimalBuilder()
                .id(2L)
                .chronicleId("2000")
                .build()
                .getEntity();
            List<MediaEntity> mediaEntities = List.of(media1, media2);

            // When
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                           () -> removeAdminActionComponent.removeAdminActionFrom(mediaEntities));

            // Then
            assertEquals("All media versions must have the same chronicle id", exception.getMessage());
        }

    }

}