package uk.gov.hmcts.darts.audio.deleter.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalOutboundDataStoreDeleterWithBufferIntTest extends IntegrationBase {

    private final String blobId = UUID.randomUUID().toString();
    private MediaRequestEntity downloadMediaRequestEntity;
    private TransientObjectDirectoryEntity transientObjectDirectoryEntity;
    private Integer transformedMediaId;

    @Autowired
    private ExternalOutboundDataStoreDeleterWithBuffer deleter;

    @BeforeEach
    void setUp() {

        UserAccountEntity requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        downloadMediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        ObjectRecordStatusEntity datastoreDeletionStatus = EodHelper.datastoreDeletionStatus();
        transientObjectDirectoryEntity = dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(dartsDatabase.getTransientObjectDirectoryStub().createTransientObjectDirectoryEntity(
                downloadMediaRequestEntity,
                datastoreDeletionStatus,
                blobId
            ));

        transformedMediaId = transientObjectDirectoryEntity.getTransformedMedia().getId();

    }

    @Test
    void deleteExpiredTransientObjectEntities_shouldDeleteData_whereTransformedMediaExpired() {
        TransformedMediaEntity transformedMediaEntity =
            dartsDatabase.getTransformedMediaRepository().findById(transformedMediaId).orElseThrow();
        transformedMediaEntity.setExpiryTime(OffsetDateTime.now().minus(Duration.ofDays(90)));
        dartsDatabase.save(transformedMediaEntity);
        assertThat(dartsDatabase.getTransientObjectDirectoryRepository().getReferenceById(transientObjectDirectoryEntity.getId()))
            .isNotNull();
        assertThat(dartsDatabase.getTransformedMediaRepository().findByMediaRequestId(downloadMediaRequestEntity.getId())).isNotEmpty();
        assertThat(dartsDatabase.getTransformedMediaRepository().getReferenceById(transformedMediaId)).isNotNull();
        int batchSize = 10;

        deleter.deleteExpiredTransientObjectEntities(batchSize);

        // Confirm that the TransientObjectDirectoryEntity and TransformedMediaEntity is deleted
        assertThat(dartsDatabase.getTransientObjectDirectoryRepository().findById(transientObjectDirectoryEntity.getId())).isEmpty();
        assertThat(dartsDatabase.getTransformedMediaRepository().findByMediaRequestId(downloadMediaRequestEntity.getId())).isEmpty();
        assertThat(dartsDatabase.getTransformedMediaRepository().findById(transformedMediaId)).isEmpty();
    }

    @Test
    void deleteExpiredTransientObjectEntities_shouldNotDeleteData_whereTransformedMediaNotExpired() {
        TransformedMediaEntity transformedMediaEntity =
            dartsDatabase.getTransformedMediaRepository().findById(transformedMediaId).orElseThrow();
        transformedMediaEntity.setExpiryTime(OffsetDateTime.now().minus(Duration.ofDays(1)));
        dartsDatabase.save(transformedMediaEntity);
        assertThat(dartsDatabase.getTransientObjectDirectoryRepository().getReferenceById(transientObjectDirectoryEntity.getId()))
            .isNotNull();
        assertThat(dartsDatabase.getTransformedMediaRepository().findByMediaRequestId(downloadMediaRequestEntity.getId())).isNotEmpty();
        assertThat(dartsDatabase.getTransformedMediaRepository().getReferenceById(transformedMediaId)).isNotNull();
        int batchSize = 10;

        deleter.deleteExpiredTransientObjectEntities(batchSize);

        // Confirm that the TransientObjectDirectoryEntity and TransformedMediaEntity is not deleted
        assertThat(dartsDatabase.getTransientObjectDirectoryRepository().findById(transientObjectDirectoryEntity.getId())).isNotNull();
        assertThat(dartsDatabase.getTransformedMediaRepository().findByMediaRequestId(downloadMediaRequestEntity.getId())).isNotEmpty();
        assertThat(dartsDatabase.getTransformedMediaRepository().findById(transformedMediaId)).isNotNull();
    }

}
