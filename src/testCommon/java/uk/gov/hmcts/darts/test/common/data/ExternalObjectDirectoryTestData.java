package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomExternalObjectDirectoryEntity;

import java.time.OffsetDateTime;

import static java.util.UUID.randomUUID;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.ExternalLocationTypeTestData.locationTypeOf;
import static uk.gov.hmcts.darts.test.common.data.ObjectRecordStatusTestData.statusOf;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class ExternalObjectDirectoryTestData implements Persistable<CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryuilderRetrieve> {

    ExternalObjectDirectoryTestData() {

    }

    /**
     * Deprectated.
     * @deprecated do not use. Instead, use fromSpec() to create an object with the desired state.
     */
    @SuppressWarnings("SummaryJavadoc")
    @Deprecated
    public ExternalObjectDirectoryEntity minimalExternalObjectDirectory() {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setStatus(statusOf(STORED));
        externalObjectDirectory.setExternalLocationType(locationTypeOf(UNSTRUCTURED));
        externalObjectDirectory.setExternalLocation(randomUUID());
        externalObjectDirectory.setVerificationAttempts(1);
        var userAccount = minimalUserAccount();
        externalObjectDirectory.setCreatedBy(userAccount);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        return externalObjectDirectory;
    }

    @Override
    public CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryuilderRetrieve someMinimal() {
        CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryuilderRetrieve builder
            = new CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryuilderRetrieve();
        var userAccount = minimalUserAccount();

        builder.getBuilder()
            .media(PersistableFactory.getMediaTestData().someMinimal().build())
            .status(ObjectRecordStatusTestData.statusOf(STORED))
            .externalLocationType(locationTypeOf(UNSTRUCTURED))
            .externalLocation(randomUUID())
            .verificationAttempts(0)
            .createdBy(userAccount)
            .lastModifiedBy(userAccount)
            .createdDateTime(OffsetDateTime.now())
            .lastModifiedDateTime(OffsetDateTime.now());

        return builder;
    }

    @Override
    public CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryuilderRetrieve someMaximal() {
        return someMinimal();
    }

    public ExternalObjectDirectoryEntity eodStoredInUnstructuredLocationForMedia(MediaEntity media) {
        CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryuilderRetrieve eod = someMinimal();
        CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryEntityBuilder builder = eod.getBuilder();

        builder.media(media).status(statusOf(STORED))
            .externalLocationType(locationTypeOf(UNSTRUCTURED))
            .externalLocation(randomUUID());
        return eod.build().getEntity();
    }
}