package uk.gov.hmcts.darts.datamanagement;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor", "PMD.MissingStaticMethodInNonInstantiatableClass"})
public class DataManagementConstants {

    @UtilityClass
    public class MetaDataNames {
        public static final String MEDIA_REQUEST_ID = "media_request_id";
        public static final String TRANSFORMED_MEDIA_ID = "transformed_media_id";
        public static final String TRANSCRIPTION_ID = "transcription_id";

    }
}
