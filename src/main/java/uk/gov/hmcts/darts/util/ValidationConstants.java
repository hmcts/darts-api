package uk.gov.hmcts.darts.util;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor", "java:S1118", "PMD.MissingStaticMethodInNonInstantiatableClass"})
public class ValidationConstants {

    public static class  MaxValues {
        public static final Long MAX_LONG_VALUE = 9_223_372_036_854_775_807L;
    }
}
