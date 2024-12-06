package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class CourtValidationUtilsTest {


    @Test
    void isUppercase_shouldReturnTrue_whenStringIsNull() {
        Assertions.assertTrue(CourtValidationUtils.isUppercase(null));
    }

    @Test
    void isUppercase_shouldReturnTrue_whenStringIsUppercase() {
        Assertions.assertTrue(CourtValidationUtils.isUppercase("ABC"));
    }

    @Test
    void isUppercase_shouldReturnFalse_whenStringContainsLowercase() {
        Assertions.assertFalse(CourtValidationUtils.isUppercase("abc"));
    }

    @Test
    void isUppercase_shouldReturnTrue_whenBothStringsAreNull() {
        Assertions.assertTrue(CourtValidationUtils.isUppercase(null, null));
    }

    @Test
    void isUppercase_shouldReturnTrue_whenBothStringsAreUppercase() {
        Assertions.assertTrue(CourtValidationUtils.isUppercase("ABC", "DEF"));
    }

    @Test
    void isUppercase_shouldReturnFalse_whenFirstStringContainsLowercase() {
        Assertions.assertFalse(CourtValidationUtils.isUppercase("abc", "DEF"));
    }

    @Test
    void isUppercase_shouldReturnFalse_whenSecondStringContainsLowercase() {
        Assertions.assertFalse(CourtValidationUtils.isUppercase("ABC", "def"));
    }
}
