package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtroomStub;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;
import uk.gov.hmcts.darts.testutils.stubs.ObjectAdminActionStub;
import uk.gov.hmcts.darts.testutils.stubs.ObjectHiddenReasonStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionDocumentStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectAdminActionRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private ObjectAdminActionRepository repository;

    @Autowired
    private MediaStub mediaStub;

    @Autowired
    private UserAccountStub userAccountStub;

    @Autowired
    private CourtroomStub courtroomStub;

    @Autowired
    private ObjectAdminActionStub objectAdminActionStub;

    @Autowired
    private ObjectHiddenReasonStub objectHiddenReasonStub;

    @Autowired
    private TranscriptionDocumentStub transcriptionDocumentStub;

    @Test
    void findAllMediaActionsWithAnyDeletionReasonShouldReturnEmptyListWhenThereAreNoMatches() {
        // When
        List<ObjectAdminActionEntity> allWithAnyDeletionReason = repository.findAllMediaActionsWithAnyDeletionReason();

        // Then
        assertEquals(0, allWithAnyDeletionReason.size());
    }

    @Test
    void findAllWithAnyDeletionReasonShouldReturnExpectedResultsWhenMediaExistsMediaActionsWithDeletionReasonButNotYetApprovedForDeletion() {
        // Given
        var courtroomEntity = courtroomStub.createCourtroomUnlessExists("Test Courthouse", "Test Courtroom",
                                                                        userAccountStub.getSystemUserAccountEntity());

        // And a media with no linked object admin action (i.e. not hidden for marked for deletion)
        createAndSaveMediaEntity(courtroomEntity);

        // And a media that's hidden only
        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .media(createAndSaveMediaEntity(courtroomEntity))
                                                .objectHiddenReason(objectHiddenReasonStub.getAnyWithMarkedForDeletion(false))
                                                .markedForManualDeletion(false)
                                                .markedForManualDelBy(null)
                                                .markedForManualDelDateTime(null)
                                                .build());

        // And a media that's marked for deletion and approved for deletion (marked for manual deletion)
        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .media(createAndSaveMediaEntity(courtroomEntity))
                                                .objectHiddenReason(objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                .markedForManualDeletion(true)
                                                .build());

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

        // And something that is not a media, but has an associated object admin action
        var transcriptionDocument = transcriptionDocumentStub.generateTranscriptionEntities(1, 1, 1,
                                                                                            false, false, false)
            .getFirst();
        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .transcriptionDocument(transcriptionDocument)
                                                .build());

        // When
        List<ObjectAdminActionEntity> result = repository.findAllMediaActionsWithAnyDeletionReason();

        // Then
        assertEquals(1, result.size());
        assertEquals(expectedObjectAdminActionEntity.getId(), result.getFirst().getId());
    }

    @Test
    void findByMediaIdAndMarkedForManualDeletionTrue() {
        var media = dartsDatabase.getMediaStub().createAndSaveMedia();
        var markedForManualDeletionAction = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                                                    .media(media)
                                                                                    .objectHiddenReason(
                                                                                        objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                                                    .markedForManualDeletion(true)
                                                                                    .build());
        objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .media(media)
                                                .objectHiddenReason(
                                                    objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                .markedForManualDeletion(false)
                                                .build());


        // When
        List<ObjectAdminActionEntity> result = repository.findByMediaIdAndMarkedForManualDeletionTrue(media.getId());

        // Then
        assertEquals(1, result.size());
        assertEquals(markedForManualDeletionAction.getId(), result.getFirst().getId());
    }

    @Test
    void testFindFilesForManualDeletion() {
        // Setup
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime deletionThreshold = now.minusDays(1);

        // Create media entities
        var courtroomEntity = courtroomStub.createCourtroomUnlessExists("Test Courthouse", "Test Courtroom",
                                                                        userAccountStub.getSystemUserAccountEntity());
        var media1 = createAndSaveMediaEntity(courtroomEntity);
        var media2 = createAndSaveMediaEntity(courtroomEntity);

        // Create transcription document entities
        var transcriptionDocument1 = transcriptionDocumentStub.generateTranscriptionEntities(1, 1, 1, false, false, false).getFirst();
        var transcriptionDocument2 = transcriptionDocumentStub.generateTranscriptionEntities(1, 1, 1, false, false, false).getFirst();

        // Create ObjectAdminActionEntity instances
        var action1 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .media(media1)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .build());

        var action2 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .media(media2)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .build());

        var action3 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .transcriptionDocument(transcriptionDocument1)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .build());

        var action4 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .transcriptionDocument(transcriptionDocument2)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .build());

        var action5 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .media(media1)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.plusDays(1))
                                                              .build());

        // Execute the method under test
        List<ObjectAdminActionEntity> result = repository.findFilesForManualDeletion(deletionThreshold, Limit.of(1000));

        // Verify the results
        assertEquals(4, result.size());
        assertTrue(result.stream().anyMatch(action -> action.getId().equals(action1.getId())));
        assertTrue(result.stream().anyMatch(action -> action.getId().equals(action2.getId())));
        assertTrue(result.stream().anyMatch(action -> action.getId().equals(action3.getId())));
        assertTrue(result.stream().anyMatch(action -> action.getId().equals(action4.getId())));

        assertTrue(result.stream().noneMatch(action -> action.getId().equals(action5.getId())));
    }


    @Test
    void testFindFilesForManualDeletionOnlyMedia() {
        // Setup
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime deletionThreshold = now.minusDays(1);

        // Create media entities
        var courtroomEntity = courtroomStub.createCourtroomUnlessExists("Test Courthouse", "Test Courtroom",
                                                                        userAccountStub.getSystemUserAccountEntity());
        var media1 = createAndSaveMediaEntity(courtroomEntity);
        var media2 = createAndSaveMediaEntity(courtroomEntity);
        var media3 = createAndSaveMediaEntity(courtroomEntity);
        media3.setDeleted(true);
        dartsPersistence.save(media3);

        // Create ObjectAdminActionEntity instances
        var action1 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .media(media1)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .build());

        var action2 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .media(media2)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .build());

        var action3 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .media(media1)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.plusDays(1))
                                                              .build());

        var action4 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .media(media3)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .build());

        // Execute the method under test
        List<ObjectAdminActionEntity> result = repository.findFilesForManualDeletion(deletionThreshold, Limit.of(1000));

        // Verify the results
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(action -> action.getId().equals(action1.getId())));
        assertTrue(result.stream().anyMatch(action -> action.getId().equals(action2.getId())));

        assertTrue(result.stream().noneMatch(action -> action.getId().equals(action3.getId())));
        assertTrue(result.stream().noneMatch(action -> action.getId().equals(action4.getId())));
    }

    @Test
    void testFindFilesForManualDeletionOnlyTranscriptionDocuments() {
        // Setup
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime deletionThreshold = now.minusDays(1);

        // Create media entities
        var courtroomEntity = courtroomStub.createCourtroomUnlessExists("Test Courthouse", "Test Courtroom",
                                                                        userAccountStub.getSystemUserAccountEntity());
        // Create transcription document entities
        var transcriptionDocument1 = transcriptionDocumentStub.generateTranscriptionEntities(1, 1, 1, false, false, false).getFirst();
        var transcriptionDocument2 = transcriptionDocumentStub.generateTranscriptionEntities(1, 1, 1, false, false, false).getFirst();
        var transcriptionDocument3 = transcriptionDocumentStub.generateTranscriptionEntities(1, 1, 1, false, false, false).getFirst();
        transcriptionDocument3.setDeleted(true);
        dartsPersistence.save(transcriptionDocument3);
        // Create ObjectAdminActionEntity instances

        var action1 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .transcriptionDocument(transcriptionDocument1)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .build());

        var action2 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .transcriptionDocument(transcriptionDocument2)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .build());

        var action3 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .transcriptionDocument(transcriptionDocument1)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.plusDays(1))
                                                              .build());

        var action4 = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                              .transcriptionDocument(transcriptionDocument3)
                                                              .markedForManualDeletion(true)
                                                              .markedForManualDelBy(userAccountStub.getSystemUserAccountEntity())
                                                              .markedForManualDelDateTime(deletionThreshold.minusDays(1))
                                                              .build());
        // Execute the method under test
        List<ObjectAdminActionEntity> result = repository.findFilesForManualDeletion(deletionThreshold, Limit.of(1000));

        // Verify the results
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(action -> action.getId().equals(action1.getId())));
        assertTrue(result.stream().anyMatch(action -> action.getId().equals(action2.getId())));

        assertTrue(result.stream().noneMatch(action -> action.getId().equals(action3.getId())));
        assertTrue(result.stream().noneMatch(action -> action.getId().equals(action3.getId())));
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
