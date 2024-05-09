package uk.gov.hmcts.darts.common.util;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class StringUtils {

    public String toScreamingSnakeCase(String string) {
        return string.replace(" ", "_")
            .toUpperCase(Locale.getDefault());
    }

}
