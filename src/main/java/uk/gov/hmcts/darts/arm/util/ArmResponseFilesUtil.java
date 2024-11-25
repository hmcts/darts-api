package uk.gov.hmcts.darts.arm.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_FILE_EXTENSION;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
@Slf4j
public class ArmResponseFilesUtil {

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

    public static String getObjectTypeId(ExternalObjectDirectoryEntity externalObjectDirectory) {
        String objectTypeId = "";
        if (nonNull(externalObjectDirectory.getMedia())) {
            objectTypeId = String.valueOf(externalObjectDirectory.getMedia().getId());
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            objectTypeId = String.valueOf(externalObjectDirectory.getTranscriptionDocumentEntity().getId());
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            objectTypeId = String.valueOf(externalObjectDirectory.getAnnotationDocumentEntity().getId());
        } else if (nonNull(externalObjectDirectory.getCaseDocument())) {
            objectTypeId = String.valueOf(externalObjectDirectory.getCaseDocument().getId());
        }
        return objectTypeId;
    }

    public static Integer getRelationIdFromArmResponseFilesInputField(String input) {
        try {
            String relationId = findFirstPatternMatch("\"relation_id\".*?:.*?\"([^\"]+)\"", input);
            return Integer.parseInt(relationId);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        }
        return null;
    }

    public static String getOperationFromArmResponseFilesInputField(String input) {
        return findFirstPatternMatch("\"operation_id\".*?:.*?\"([^\"]+)\"", input);
    }

    public static String findFirstPatternMatch(String regex, String text) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
