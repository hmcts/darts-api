package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@SuppressWarnings({"PMD.LawOfDemeter", "HideUtilityClassConstructor"})
public class TransientObjectDirectoryTestData {

    public static TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(
        MediaRequestEntity mediaRequestEntity,
        ObjectDirectoryStatusEntity objectDirectoryStatusEntity,
        UUID externalLocation) {
        var now = OffsetDateTime.now();
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setMediaRequest(mediaRequestEntity);
        transientObjectDirectoryEntity.setStatus(objectDirectoryStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(externalLocation);
        transientObjectDirectoryEntity.setCreatedTimestamp(now);
        transientObjectDirectoryEntity.setCreatedBy(mediaRequestEntity.getCreatedBy());
        transientObjectDirectoryEntity.setModifiedTimestamp(now);
        transientObjectDirectoryEntity.setModifiedBy(mediaRequestEntity.getModifiedBy());

        return transientObjectDirectoryEntity;
    }

}
