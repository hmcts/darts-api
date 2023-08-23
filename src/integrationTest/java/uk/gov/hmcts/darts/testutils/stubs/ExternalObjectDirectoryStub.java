package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExternalObjectDirectoryStub {
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final UserAccountStub userAccountStub;

    public ExternalObjectDirectoryEntity someMinimalExternalObjectDirectory() {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();

        var externalLocationType = new ExternalLocationTypeEntity();
        externalLocationType.setId(ExternalLocationTypeEnum.INBOUND.getId());
        externalObjectDirectory.setExternalLocationType(externalLocationType);

        var objectDirectoryStatus = new ObjectDirectoryStatusEntity();
        objectDirectoryStatus.setId(ObjectDirectoryStatusEnum.AWAITING_VERIFICATION.getId());

        externalObjectDirectory.setStatus(objectDirectoryStatus);

        return externalObjectDirectory;
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity mediaEntity,
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

        externalObjectDirectory.setLastModifiedBy(userAccountStub.getDefaultUser());

        return externalObjectDirectory;
    }

}
