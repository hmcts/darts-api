package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;

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

    public static String removeIds(String input) {
        return input.replaceAll("\"case_id\".{1,6},", "")
            .replaceAll("\"id\".{1,6},", "");
    }

    public static String removeTags(List<String> tagsToRemove, String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            for (String tagToRemove : tagsToRemove) {
                List<JsonNode> parentNodes = jsonNode.findParents(tagToRemove);
                for (JsonNode parentNode : parentNodes) {
                    ((ObjectNode) parentNode).remove(tagToRemove);
                }
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }

    public static String substituteHearingDateWithToday(String expectedResponse) {
        return expectedResponse.replace("todays_date", LocalDate.now().toString());
    }

    public static void compareJson(String expectedJson, String actualJson, List<String> tagsToRemove) {
        JSONAssert.assertEquals(removeTags(tagsToRemove, expectedJson), removeTags(tagsToRemove, actualJson), JSONCompareMode.NON_EXTENSIBLE);
    }
}
