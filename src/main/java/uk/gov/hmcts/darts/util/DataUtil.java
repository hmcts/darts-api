package uk.gov.hmcts.darts.util;

import java.util.Optional;

public final class DataUtil {
    private DataUtil() {

    }

    public static String toUpperCase(String value) {
        return Optional.ofNullable(value).map(String::toUpperCase).orElse(null);
    }
}
