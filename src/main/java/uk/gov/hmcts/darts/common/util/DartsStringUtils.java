package uk.gov.hmcts.darts.common.util;

import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class DartsStringUtils {

    public String toScreamingSnakeCase(String string) {
        return string.replace(" ", "_")
            .toUpperCase(Locale.getDefault());
    }

}
