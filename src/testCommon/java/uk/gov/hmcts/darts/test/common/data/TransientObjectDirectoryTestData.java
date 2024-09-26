package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.time.OffsetDateTime;

import static java.util.UUID.randomUUID;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.ObjectRecordStatusTestData.statusOf;
import static uk.gov.hmcts.darts.test.common.data.TransformedMediaTestData.minimalTransformedMedia;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public class TransientObjectDirectoryTestData {

    private TransientObjectDirectoryTestData() {

    }

    public static TransientObjectDirectoryEntity minimalTransientObjectDirectory() {
        var externalObjectDirectory = new TransientObjectDirectoryEntity();
        externalObjectDirectory.setStatus(statusOf(STORED));
        externalObjectDirectory.setExternalLocation(randomUUID());
        externalObjectDirectory.setCreatedDateTime(OffsetDateTime.now());
        externalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectory.setTransformedMedia(minimalTransformedMedia());
        var userAccount = minimalUserAccount();
        externalObjectDirectory.setCreatedBy(userAccount);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        return externalObjectDirectory;
    }
}