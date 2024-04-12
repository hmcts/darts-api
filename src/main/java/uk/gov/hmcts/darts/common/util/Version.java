package uk.gov.hmcts.darts.common.util;

import lombok.extern.slf4j.Slf4j;


/**
 * A class that manages the versioning format within modernised darts
 * TODO: Understand how this version number should be represented
 **/
@Slf4j
public class Version implements Comparable<Version>{

    private Integer major = 1;

    private Integer minor = 0;

    private static final String DELIMINTER = "/*\\.";

    public Version() {
    }

    public Version(String version) {
        String[] versionSplit = null;
        if (!version.isEmpty()) {
            versionSplit = version.split(DELIMINTER);
        }

        if (versionSplit != null && versionSplit.length == 2) {
            try {
                major = Integer.parseInt(versionSplit[0]);
            } catch (NumberFormatException exception) {
                log.info("Could not find major number", exception);
            }

            try {
                minor = Integer.parseInt(versionSplit[1]);
            } catch (NumberFormatException exception) {
                log.info("Could not find minor number", exception);
            }
        }
    }

    @Override
    public int compareTo(Version o) {
        if (major == major && minor == minor) {
            return 0;
        }

        if (major > o.major) {
            return 1;
        } else if (major < o.major) {
            return -1;
        }

        if (minor > o.minor) {
            return 1;
        } else if (minor < o.minor) {
            return -1;
        }

        return 0;
    }

    public void increaseMajor() {
        major = major + 1;
        minor = 0;
    }

    public void increaseMinor() {
        minor = minor + 1;
    }

    public String getVersionString() {
        return major + "." + minor;
    }
}