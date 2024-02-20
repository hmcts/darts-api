package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class ExternalObjectDirectoryTestData {


    public static final int TEST_EXTERNAL_OBJECT_DIRECTORY_ID = 123;
    public static final String TEST_CHECKSUM = "wysXTgRikGN6nMB8AJ0JrQ==";
    public static final OffsetDateTime CREATED_DATE_TIME = OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    public static final OffsetDateTime LAST_MODIFIED_DATE_TIME = OffsetDateTime.of(2023, 6, 20, 11, 0, 0, 0, ZoneOffset.UTC);

    public static ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity media,
                                                                              ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                              ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                              UUID externalLocation) {

        return createExternalObjectDirectory(media,
                                             ObjectRecordStatusTestData.getObjectRecordStatus(objectRecordStatusEnum),
                                             ExternalLocationTypeTestData.getExternalLocationType(externalLocationTypeEnum),
                                             externalLocation);
    }

    public static ExternalObjectDirectoryEntity createExternalObjectDirectory(AnnotationDocumentEntity annotationDocumentEntity,
                                                                              ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                              ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                              UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setAnnotationDocumentEntity(annotationDocumentEntity);

        return externalObjectDirectory;
    }


    public static ExternalObjectDirectoryEntity createExternalObjectDirectory(TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                                              ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                              ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                              UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setTranscriptionDocumentEntity(transcriptionDocumentEntity);

        return externalObjectDirectory;
    }

    public static ExternalObjectDirectoryEntity createMinimalExternalObjectDirectory(ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                                     ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                                     UUID externalLocation) {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setId(TEST_EXTERNAL_OBJECT_DIRECTORY_ID);
        externalObjectDirectory.setStatus(objectRecordStatusEntity);
        externalObjectDirectory.setExternalLocationType(externalLocationTypeEntity);
        externalObjectDirectory.setExternalLocation(externalLocation);
        externalObjectDirectory.setChecksum(TEST_CHECKSUM);
        externalObjectDirectory.setTransferAttempts(1);
        externalObjectDirectory.setVerificationAttempts(1);

        var user = UserAccountTestData.minimalUserAccount();
        externalObjectDirectory.setCreatedBy(user);
        externalObjectDirectory.setCreatedDateTime(CREATED_DATE_TIME);
        externalObjectDirectory.setLastModifiedBy(user);
        externalObjectDirectory.setLastModifiedDateTime(LAST_MODIFIED_DATE_TIME);
        return externalObjectDirectory;
    }

    private ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity mediaEntity,
                                                                        ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                        ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                        UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setMedia(mediaEntity);

        return externalObjectDirectory;
    }
}
