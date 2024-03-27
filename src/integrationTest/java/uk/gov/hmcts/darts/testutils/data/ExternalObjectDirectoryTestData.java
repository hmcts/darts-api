package uk.gov.hmcts.darts.testutils.data;

import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import static java.util.UUID.randomUUID;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.testutils.data.ExternalLocationTypeTestData.locationTypeOf;
import static uk.gov.hmcts.darts.testutils.data.ObjectRecordStatusTestData.statusOf;
import static uk.gov.hmcts.darts.testutils.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class ExternalObjectDirectoryTestData {

    public static ExternalObjectDirectoryEntity minimalExternalObjectDirectory() {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setStatus(statusOf(STORED));
        externalObjectDirectory.setExternalLocationType(locationTypeOf(UNSTRUCTURED));
        externalObjectDirectory.setExternalLocation(randomUUID());
        externalObjectDirectory.setVerificationAttempts(1);
        externalObjectDirectory.setCreatedBy(minimalUserAccount());
        externalObjectDirectory.setLastModifiedBy(minimalUserAccount());
        return externalObjectDirectory;
    }
}
