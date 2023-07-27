package uk.gov.hmcts.darts.testutils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.LocalDate;

@SuppressWarnings({"PMD.TestClassWithoutTestCases"})
public final class TestUtils {

    private TestUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getContentsFromFile(String filelocation) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(filelocation);
        if (resource == null) {
            throw new IOException(MessageFormat.format("File not found {0}", filelocation));
        }
        File file = new File(resource.getFile());
        return FileUtils.readFileToString(file, "UTF-8");

    }

    public static String substituteHearingDateWithToday(String expectedResponse) {
        return expectedResponse.replace("todays_date", LocalDate.now().toString());
    }
}
