package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExternalObjectDirectoryStub {

    private final UserAccountStub userAccountStub;

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

        var user = userAccountStub.getIntegrationTestUserAccountEntity();
        externalObjectDirectory.setCreatedBy(user);
        externalObjectDirectory.setLastModifiedBy(user);

        return externalObjectDirectory;
    }

}
