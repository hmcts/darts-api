package uk.gov.hmcts.darts.arm.util;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor", "java:S1118"})
public class ArchiveConstants {

    public static class ArchiveRecordOperationValues {
        public static final String CREATE_RECORD = "create_record";
        public static final String UPLOAD_NEW_FILE = "upload_new_file";
        public static final String INVALID_LINE = "invalid_line";
        public static final String ARM_FILENAME_SEPARATOR = "_";
    }

    public static class ArchiveMapperValues {
        public static final String TRANSCRIPTION_REQUEST_MANUAL = "Manual";
        public static final String TRANSCRIPTION_REQUEST_AUTOMATIC = "Automatic";
    }

}
