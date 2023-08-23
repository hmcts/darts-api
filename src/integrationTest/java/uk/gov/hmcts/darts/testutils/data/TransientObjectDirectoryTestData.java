package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@SuppressWarnings({"PMD.LawOfDemeter", "HideUtilityClassConstructor"})
public class TransientObjectDirectoryTestData {

    public static TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(MediaRequestEntity mediaRequestEntity,
                                                                                      ObjectDirectoryStatusEntity objectDirectoryStatusEntity,
                                                                                      UUID externalLocation) {
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setMediaRequest(mediaRequestEntity);
        transientObjectDirectoryEntity.setLastModifiedBy(new UserAccountEntity());
        transientObjectDirectoryEntity.setStatus(objectDirectoryStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(externalLocation);
        transientObjectDirectoryEntity.setLastModifiedDateTime(OffsetDateTime.now());

        return transientObjectDirectoryEntity;
    }

}
