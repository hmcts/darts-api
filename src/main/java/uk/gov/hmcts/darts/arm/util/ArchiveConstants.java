package uk.gov.hmcts.darts.arm.util;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor", "java:S1118", "PMD.MissingStaticMethodInNonInstantiatableClass"})
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

    public static class ArchiveResponseFileAttributes {
        public static final String ARM_RESPONSE_FILE_EXTENSION = ".rsp";
        public static final String ARM_INPUT_UPLOAD_FILENAME_KEY = "iu";
        public static final String ARM_CREATE_RECORD_FILENAME_KEY = "cr";
        public static final String ARM_UPLOAD_FILE_FILENAME_KEY = "uf";
        public static final String ARM_INVALID_LINE_FILENAME_KEY = "il";
        public static final String ARM_RESPONSE_SUCCESS_STATUS_CODE = "1";
        public static final String ARM_RESPONSE_INVALID_STATUS_CODE = "0";

    }
}
