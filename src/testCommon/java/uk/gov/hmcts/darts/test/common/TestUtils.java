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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.assertj.core.data.TemporalUnitOffset;
import org.hibernate.LazyInitializationException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.zalando.problem.jackson.ProblemModule;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.task.runner.HasId;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.task.runner.HasLongId;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;

import static java.lang.Character.isLetter;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases",
    "PMD.CognitiveComplexity",
    "PMD.GodClass"
})
@Slf4j
public final class TestUtils {

    public static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    public static final int AUTOMATION_USER_ID = 0;
    public static final TemporalUnitOffset TIME_TOLERANCE = within(5, ChronoUnit.SECONDS);

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
        objectMapper.registerModule(new ProblemModule());

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
            String line = reader.readLine();
            StringBuilder content = new StringBuilder();
            while (line != null) {
                content.append(line);
                line = reader.readLine();
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
                while (!parentNodes.isEmpty()) {
                    for (JsonNode parentNode : parentNodes) {
                        ((ObjectNode) parentNode).remove(tagToRemove);
                    }
                    parentNodes = jsonNode.findParents(tagToRemove);
                }
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            return input.replaceAll("\"" + String.join("|", tagsToRemove) + "\".+?,", "")
                .replaceAll("\"" + String.join("|", tagsToRemove) + "\".+?}", "}")
                .replaceAll("\"" + String.join("|", tagsToRemove) + "\".+?\\n", "\n");
        }
    }

    public static void compareJson(String expectedJson, String actualJson, List<String> tagsToRemove, JSONCompareMode jsonCompareMode) {
        JSONAssert.assertEquals(removeTags(tagsToRemove, expectedJson), removeTags(tagsToRemove, actualJson), jsonCompareMode);
    }

    public static void compareJson(String expectedJson, String actualJson, List<String> tagsToRemove) {
        compareJson(expectedJson, actualJson, tagsToRemove, JSONCompareMode.NON_EXTENSIBLE);
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

    public static String writeAsString(Object object) throws JsonProcessingException {
        if (object instanceof String) {
            return String.valueOf(object);
        }
        return getObjectMapper().writeValueAsString(object);
    }

    @SuppressWarnings("PMD.DoNotUseThreads")//Required to avoid busy waiting
    public static void retryLoop(int maxRetries, int waitBetweenTries, Runnable runnable) {
        int currentRetry = 0;
        do {
            try {
                runnable.run();
                break;
            } catch (Exception e) {
                currentRetry++;
                if (currentRetry > maxRetries) {
                    throw e;
                }
                log.error("Retry Loop, run failed with exception. Try count {} out of {}", currentRetry, maxRetries, e);
                try {
                    Thread.sleep(waitBetweenTries);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        } while (maxRetries > 0);
    }

    public static boolean isProxy(Collection<?> collection) {
        try {
            collection.size();
            return false;
        } catch (LazyInitializationException e) {
            return true;
        }
    }

    public static <T extends CreatedBy & HasIntegerId> T getFirstInt(Collection<T> data) {
        return getFirst(data, (o1, o2) -> Integer.compare(o1.getId(), o2.getId()));
    }

    public static <T extends CreatedBy & HasLongId> T getFirstLong(Collection<T> data) {
        return getFirst(data, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));
    }

    public static <T extends CreatedBy & HasId<?>> T getFirst(Collection<T> data,
                                                              BiFunction<T, T, Integer> compareFunction) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return getOrderedByCreatedByAndId(data, compareFunction).getFirst();
    }


    public static <T extends CreatedBy & HasIntegerId> List<T> getOrderedByCreatedByAndIdInt(Collection<T> data) {
        return getOrderedByCreatedByAndId(data, (o1, o2) -> Integer.compare(o1.getId(), o2.getId()));
    }

    public static <T extends CreatedBy & HasLongId> List<T> getOrderedByCreatedByAndIdLong(Collection<T> data) {
        return getOrderedByCreatedByAndId(data, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

    }

    public static <T extends CreatedBy & HasId<?>> List<T> getOrderedByCreatedByAndId(Collection<T> data,
                                                                                      BiFunction<T, T, Integer> compareFunction) {
        List<T> sortedData = new ArrayList<>();
        if (CollectionUtils.isEmpty(data)) {
            return sortedData;
        }
        sortedData.addAll(
            data.stream()
                .filter(Objects::nonNull)
                .filter(entity -> entity.getId() != null)
                .filter(entity -> entity.getCreatedDateTime() != null)
                .sorted((o1, o2) -> {
                    int compare = o1.getCreatedDateTime().compareTo(o2.getCreatedDateTime());
                    if (compare == 0) {
                        return compareFunction.apply(o1, o2);
                    }
                    return compare;
                })
                .toList());
        // Add entities without id or createdDateTime at the end
        sortedData.addAll(
            data.stream()
                .filter(Objects::nonNull)
                .filter(entity -> entity.getId() == null || entity.getCreatedDateTime() == null)
                .toList());
        if (sortedData.isEmpty() && !data.isEmpty()) {
            return new ArrayList<>(data);
        }
        return sortedData;
    }
}