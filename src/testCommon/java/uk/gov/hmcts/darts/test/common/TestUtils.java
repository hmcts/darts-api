package uk.gov.hmcts.darts.test.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Character.isLetter;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.CognitiveComplexity"})
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

    public static File getFile(String filelocation) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new File(classLoader.getResource(filelocation).getFile());
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }

    public static String substituteHearingDateWithToday(String expectedResponse) {
        return expectedResponse.replace("todays_date", LocalDate.now().toString());
    }

    public static String readTempFileContent(final String annotationsFile) {
        String xmlContent = "";

        try (BufferedReader reader = Files.newBufferedReader(Path.of(annotationsFile))) {
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            xmlContent = content.toString();
        } catch (Exception e) {
            fail("Error reading XML file: " + e.getMessage());
        }

        return xmlContent;
    }

    public static <T> T unmarshalXmlFile(Class<T> type, String xmlFile) throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(type);
        return type.cast(context.createUnmarshaller().unmarshal(Files.newBufferedReader(Path.of(xmlFile))));
    }

    public static String removeTags(List<String> tagsToRemove, String input) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(input);
            for (String tagToRemove : tagsToRemove) {
                List<JsonNode> parentNodes = jsonNode.findParents(tagToRemove);
                for (JsonNode parentNode : parentNodes) {
                    ((ObjectNode) parentNode).remove(tagToRemove);
                }
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            return input.replaceAll("\"" + String.join("|", tagsToRemove) + "\".+?,", "")
                .replaceAll("\"" + String.join("|", tagsToRemove) + "\".+?}", "}")
                .replaceAll("\"" + String.join("|", tagsToRemove) + "\".+?\\n", "\n");
        }
    }

    public static void compareJson(String expectedJson, String actualJson, List<String> tagsToRemove) {
        JSONAssert.assertEquals(removeTags(tagsToRemove, expectedJson), removeTags(tagsToRemove, actualJson), JSONCompareMode.NON_EXTENSIBLE);
    }

    public static String encodeToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static String randomizeCase(String str) {
        Random random = new Random();
        StringBuilder output = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (random.nextBoolean()) {
                if (isLetter(c)) {
                    if (isUpperCase(c)) {
                        output.append(toLowerCase(c));
                    } else {
                        output.append(toUpperCase(c));
                    }
                } else {
                    output.append(c);
                }
            } else {
                output.append(c);
            }
        }

        if (output.toString().equals(str)) {
            return randomizeCase(str);
        }

        return output.toString();
    }

    /**
     * Search byte array for the byte pattern.
     *
     * @param array   to look into
     * @param pattern to search
     * @return index of first occurrence of the pattern, -1 otherwise
     */
    public static int searchBytePattern(byte[] array, byte[] pattern) {
        if (pattern.length > array.length) {
            return -1;
        }
        for (int i = 0; i <= array.length - pattern.length; i++) {
            if (Arrays.compare(array, i, i + pattern.length, pattern, 0, pattern.length) == 0) {
                return i;
            }
        }
        return -1;
    }

    public static String removeIds(String input) {
        return input.replaceAll("\"case_id\".{1,6},", "")
            .replaceAll("\"id\".{1,6},", "");
    }
}