package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.UUID;

@UtilityClass
@SuppressWarnings({"PMD.LawOfDemeter", "HideUtilityClassConstructor"})
public class ExternalObjectDirectoryTestData {

    public static ExternalObjectDirectoryEntity someMinimalExternalObjectDirectory() {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();

        var externalLocationType = new ExternalLocationTypeEntity();
        externalLocationType.setId(ExternalLocationTypeEnum.INBOUND.getId());
        externalObjectDirectory.setExternalLocationType(externalLocationType);

        var objectDirectoryStatus = new ObjectDirectoryStatusEntity();
        objectDirectoryStatus.setId(ObjectDirectoryStatusEnum.AWAITING_VERIFICATION.getId());

        externalObjectDirectory.setStatus(objectDirectoryStatus);

        return externalObjectDirectory;
    }

    public static ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity mediaEntity,
                                                                              ObjectDirectoryStatusEntity objectDirectoryStatusEntity,
                                                                              ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                              UUID externalLocation) {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setMedia(mediaEntity);
        externalObjectDirectory.setStatus(objectDirectoryStatusEntity);
        externalObjectDirectory.setExternalLocationType(externalLocationTypeEntity);
        externalObjectDirectory.setExternalLocation(externalLocation);
        externalObjectDirectory.setChecksum(null);
        externalObjectDirectory.setTransferAttempts(null);

        UserAccountEntity user = new UserAccountEntity();
        externalObjectDirectory.setModifiedBy(user);

        return externalObjectDirectory;
    }

}
