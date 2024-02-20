package uk.gov.hmcts.darts.arm.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_FILE_EXTENSION;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class ArmResponseFilesHelper {

    public static String generateSuffix(String filenameKey) {
        return ARM_FILENAME_SEPARATOR + filenameKey + ARM_RESPONSE_FILE_EXTENSION;
    }


    public static String getPrefix(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return new StringBuilder(externalObjectDirectory.getId().toString())
                .append(ARM_FILENAME_SEPARATOR)
                .append(getObjectTypeId(externalObjectDirectory))
                .append(ARM_FILENAME_SEPARATOR)
                .append(externalObjectDirectory.getTransferAttempts()).toString();
    }

    private static String getObjectTypeId(ExternalObjectDirectoryEntity externalObjectDirectory) {
        String objectTypeId = "";
        if (nonNull(externalObjectDirectory.getMedia())) {
            objectTypeId = externalObjectDirectory.getMedia().getId().toString();
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            objectTypeId = externalObjectDirectory.getTranscriptionDocumentEntity().getId().toString();
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            objectTypeId = externalObjectDirectory.getAnnotationDocumentEntity().getId().toString();
        }
        return objectTypeId;
    }
}
