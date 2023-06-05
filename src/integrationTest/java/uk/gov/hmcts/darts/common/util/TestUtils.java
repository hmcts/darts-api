package uk.gov.hmcts.darts.common.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public final class TestUtils {

    private TestUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getContentsFromFile(String filelocation) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File file = new File(classLoader.getResource(filelocation).getFile());
        return FileUtils.readFileToString(file, "UTF-8");

    }
}
