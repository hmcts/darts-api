package uk.gov.hmcts.darts.common.util;


public final class CourtValidationUtils {

    private CourtValidationUtils() {
        // empty constructor
    }

    public static boolean isUppercase(String value) {
        return value == null || value.equals(value.toUpperCase());
    }

    public static boolean isUppercase(String courthouse, String courtroom) {
        return isUppercase(courthouse) && isUppercase(courtroom);
    }
}
