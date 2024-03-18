package uk.gov.hmcts.darts.common.util;

import org.springframework.stereotype.Component;

@Component
public class StringUtils {

    public String toScreamingSnakeCase(String string) {
        return string.replace(" ", "_")
            .toUpperCase();
    }

}
