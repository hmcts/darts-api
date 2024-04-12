package uk.gov.hmcts.darts.common.util;

import io.restassured.internal.common.assertion.Assertion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionTest {

    @Test
    public void testVersionParse() {
        Version version = new Version("2.0");
        String strVersion = version.getVersionString();

        Assertions.assertEquals("2.0", strVersion);
    }

    @Test
    public void testVersionParseUnrecognisedFormat() {
        Version version = new Version("234234324");
        String strVersion = version.getVersionString();

        Assertions.assertEquals("1.0", strVersion);
    }


    @Test
    public void testVersionIncreaseMajor() {
        Version version = new Version("1.0");
        version.increaseMajor();
        Assertions.assertEquals("2.0", version.getVersionString());
    }

    @Test
    public void testVersionIncreaseMinor() {
        Version version = new Version("1.0");
        version.increaseMinor();
        Assertions.assertEquals("1.1", version.getVersionString());
    }
}