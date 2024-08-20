package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    @BeforeEach
    void openHibernateSession() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void findAllMediaActionsWithAnyDeletionReasonShouldReturnEmptyListWhenThereAreNoMatches() {
        // When
        List<ObjectAdminActionEntity> allWithAnyDeletionReason = repository.findAllMediaActionsWithAnyDeletionReason();

        // Then
        Assertions.assertEquals(0, allWithAnyDeletionReason.size());
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
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expectedObjectAdminActionEntity.getId(), result.getFirst().getId());
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