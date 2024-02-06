package uk.gov.hmcts.darts.common.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@UtilityClass
public class PropertyFileLoader {

    public static Properties loadPropertiesFromFile(String filename) throws IOException {
        Properties customProperties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(filename)) {
            // load a properties file
            customProperties.load(input);
        }
        return customProperties;
    }
}
